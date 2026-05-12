/*
 * Shared client for on-demand LLM endpoints.
 *
 * Calls the server endpoint with fetch() and dispatches to the appropriate handler
 * depending on the response Content-Type:
 *   - text/event-stream  -> SSE; chunks delivered to onChunk(accumulated, delta)
 *   - application/json   -> single JSON payload, delivered to onJson(parsed)
 *
 * Callbacks:
 *   onChunk(accumulated, delta) - called on every streamed chunk (SSE only)
 *   onDone(accumulated)         - called when SSE stream finishes successfully
 *   onJson(parsedBody)          - called when a JSON response arrives successfully
 *   onError(message)            - called on any failure (network, HTTP, SSE error event)
 *   onStart()                   - optional, fired immediately after the request begins
 */
(function (global) {
    function parseSseEvent(raw) {
        var lines = raw.split(/\r?\n/);
        var eventName = "message";
        var dataParts = [];
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i];
            if (line.indexOf("event:") === 0) {
                eventName = line.slice(6).trim();
            } else if (line.indexOf("data:") === 0) {
                dataParts.push(line.slice(5).trim());
            }
        }
        return { event: eventName, data: dataParts.join("\n") };
    }

    function consumeStream(response, callbacks) {
        var reader = response.body.getReader();
        var decoder = new TextDecoder("utf-8");
        var buffer = "";
        var accumulated = "";
        var failed = null;
        var finishedByDone = false;

        function pump() {
            return reader.read().then(function (result) {
                if (result.done) {
                    return;
                }
                buffer += decoder.decode(result.value, { stream: true });
                var sepIndex;
                while ((sepIndex = buffer.indexOf("\n\n")) >= 0) {
                    var raw = buffer.slice(0, sepIndex);
                    buffer = buffer.slice(sepIndex + 2);
                    var evt = parseSseEvent(raw);
                    if (evt.event === "chunk" && evt.data) {
                        try {
                            var parsed = JSON.parse(evt.data);
                            if (parsed && typeof parsed.text === "string" && parsed.text.length > 0) {
                                accumulated += parsed.text;
                                if (callbacks.onChunk) {
                                    callbacks.onChunk(accumulated, parsed.text);
                                }
                            }
                        } catch (_) {
                            // Ignore malformed chunk
                        }
                    } else if (evt.event === "done") {
                        finishedByDone = true;
                    } else if (evt.event === "error") {
                        try {
                            var errPayload = JSON.parse(evt.data);
                            failed = (errPayload && errPayload.error) || "AI streaming failed.";
                        } catch (_) {
                            failed = "AI streaming failed.";
                        }
                    }
                }
                return pump();
            });
        }

        return pump().then(function () {
            if (failed) {
                if (callbacks.onError) {
                    callbacks.onError(failed);
                }
            } else if (callbacks.onDone) {
                callbacks.onDone(accumulated, finishedByDone);
            }
        });
    }

    function callLlm(url, callbacks) {
        callbacks = callbacks || {};
        if (callbacks.onStart) {
            try { callbacks.onStart(); } catch (_) {}
        }
        return fetch(url, { method: "GET", credentials: "same-origin" })
            .then(function (response) {
                var contentType = (response.headers.get("Content-Type") || "").toLowerCase();
                if (contentType.indexOf("text/event-stream") >= 0 && response.body && typeof response.body.getReader === "function") {
                    return consumeStream(response, callbacks);
                }
                return response.json().then(function (data) {
                    if (!response.ok || !data || data.ok === false) {
                        var msg = (data && data.error) ? data.error : ("HTTP " + response.status);
                        if (callbacks.onError) {
                            callbacks.onError(msg);
                        }
                        return;
                    }
                    if (callbacks.onJson) {
                        callbacks.onJson(data);
                    }
                });
            })
            .catch(function (err) {
                if (callbacks.onError) {
                    callbacks.onError((err && err.message) ? err.message : "Network error.");
                }
            });
    }

    global.LlmStream = { call: callLlm };
})(window);
