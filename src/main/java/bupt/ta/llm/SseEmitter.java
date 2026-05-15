package bupt.ta.llm;

import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Thin Server-Sent Events helper used by the streaming LLM endpoints.
 *
 * <p>Event shapes the front-end expects:
 * <ul>
 *   <li>{@code event: chunk} &mdash; payload {@code {"text":"..."}}, one per delta token</li>
 *   <li>{@code event: done}  &mdash; payload {@code {"ok":true}}</li>
 *   <li>{@code event: error} &mdash; payload {@code {"ok":false,"error":"message"}}</li>
 * </ul>
 */
public final class SseEmitter {

    private final HttpServletResponse response;
    private final PrintWriter writer;

    /**
     * Prepares the response for Server-Sent Events streaming.
     *
     * @param response servlet response to configure
     * @throws IOException if the writer cannot be opened
     */
    public SseEmitter(HttpServletResponse response) throws IOException {
        this.response = response;
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        this.writer = response.getWriter();
    }

    /**
     * @param text token delta to send as a {@code chunk} event (ignored when blank)
     */
    public void sendChunk(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("text", text);
        send("chunk", payload.toString());
    }

    /** Emits a {@code done} event with {@code {"ok":true}}. */
    public void sendDone() {
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", true);
        send("done", payload.toString());
    }

    /**
     * @param message error text for the {@code error} event payload
     */
    public void sendError(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("ok", false);
        payload.addProperty("error", message != null ? message : "Unknown error");
        send("error", payload.toString());
    }

    private void send(String event, String data) {
        writer.write("event: ");
        writer.write(event);
        writer.write("\n");
        writer.write("data: ");
        writer.write(data);
        writer.write("\n\n");
        writer.flush();
        try {
            response.flushBuffer();
        } catch (IOException ignored) {
            // Client closed the connection; subsequent writes will no-op.
        }
    }
}
