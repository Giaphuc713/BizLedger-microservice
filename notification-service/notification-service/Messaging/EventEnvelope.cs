using System.Text.Json.Serialization;

namespace notification_service.Messaging
{
    public class EventEnvelope
    {
        [JsonPropertyName("eventId")]
        public string EventId { get; set; } = string.Empty;

        [JsonPropertyName("eventType")]
        public string EventType { get; set; } = string.Empty;

        [JsonPropertyName("eventVersion")]
        public int EventVersion { get; set; } = 1;

        [JsonPropertyName("occurredAt")]
        public string OccurredAt { get; set; } = string.Empty;

        [JsonPropertyName("source")]
        public string Source { get; set; } = string.Empty;

        [JsonPropertyName("traceId")]
        public string TraceId { get; set; } = string.Empty;

        [JsonPropertyName("tenantId")]
        public string TenantId { get; set; } = string.Empty;

        [JsonPropertyName("data")]
        public System.Text.Json.JsonElement Data { get; set; }
    }
}
