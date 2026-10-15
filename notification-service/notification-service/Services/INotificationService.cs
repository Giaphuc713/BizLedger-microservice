using notification_service.Models;

namespace notification_service.Services
{
    public interface INotificationService
    {
        Task<List<Notification>> GetNotificationsAsync(Guid tenantId);
        Task<bool> MarkAsReadAsync(Guid id, Guid tenantId);
        Task MarkAllAsReadAsync(Guid tenantId);
        Task<List<WebhookSubscription>> GetWebhooksAsync(Guid tenantId);
        Task<WebhookSubscription> CreateWebhookAsync(Guid tenantId, WebhookSubscription subscription);
        Task<bool> DeleteWebhookAsync(Guid id, Guid tenantId);
        Task<List<WebhookDeliveryLog>> GetDeliveriesAsync(Guid tenantId);
        Task<Dictionary<string, object>> GetDashboardMetricsAsync(Guid tenantId);
    }
}
