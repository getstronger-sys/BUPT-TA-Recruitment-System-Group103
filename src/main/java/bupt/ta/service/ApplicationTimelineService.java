package bupt.ta.service;

import bupt.ta.model.Application;
import bupt.ta.model.ApplicationEvent;
import bupt.ta.model.Job;
import bupt.ta.storage.DataStorage;

import java.io.IOException;

/**
 * Small helper for writing application audit timeline events consistently.
 */
public class ApplicationTimelineService {
    public static final String TYPE_SUBMITTED = "SUBMITTED";
    public static final String TYPE_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String TYPE_INTERVIEW_NOTICE = "INTERVIEW_NOTICE";
    public static final String TYPE_INTERVIEW_BOOKED = "INTERVIEW_BOOKED";
    public static final String TYPE_INTERVIEW_CANCELLED = "INTERVIEW_CANCELLED";
    public static final String TYPE_EVALUATION_SAVED = "EVALUATION_SAVED";
    public static final String TYPE_DECISION_RECORDED = "DECISION_RECORDED";
    public static final String TYPE_WITHDRAWN = "WITHDRAWN";
    public static final String TYPE_AUTO_PROMOTED = "AUTO_PROMOTED";

    /** Appends one timeline event for an application. */
    public void record(DataStorage storage, Application app, Job job,
                       String actorUserId, String actorName, String actorRole,
                       String eventType, String title, String detail,
                       String fromStatus, String toStatus) throws IOException {
        if (storage == null || app == null || app.getId() == null) {
            return;
        }
        ApplicationEvent event = new ApplicationEvent();
        event.setApplicationId(app.getId());
        event.setJobId(app.getJobId());
        event.setApplicantId(app.getApplicantId());
        event.setActorUserId(clean(actorUserId));
        event.setActorName(clean(actorName));
        event.setActorRole(clean(actorRole));
        event.setEventType(clean(eventType));
        event.setTitle(clean(title));
        event.setDetail(clean(detail));
        event.setFromStatus(clean(fromStatus));
        event.setToStatus(clean(toStatus));
        storage.addApplicationEvent(event);
    }

    /** Records a {@link #TYPE_STATUS_CHANGED} timeline event. */
    public void recordStatusChange(DataStorage storage, Application app, Job job,
                                   String actorUserId, String actorName, String actorRole,
                                   String fromStatus, String toStatus, String detail) throws IOException {
        record(storage, app, job, actorUserId, actorName, actorRole,
                TYPE_STATUS_CHANGED, "Status changed", detail, fromStatus, toStatus);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
