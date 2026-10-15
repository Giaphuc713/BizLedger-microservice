using Microsoft.AspNetCore.SignalR;

namespace notification_service
{
    public class NotificationHub : Hub
    {
        // Clients connect of hubs sending X-Tenant-Id header or query string
        public override async Task OnConnectedAsync()
        {
            var httpContext = Context.GetHttpContext();
            var tenantId = httpContext?.Request.Query["tenantId"].ToString()
                           ?? httpContext?.Request.Headers["X-Tenant-Id"].ToString();

            if (!string.IsNullOrEmpty(tenantId))
            {
                // Assign connection to a tenant room/group
                await Groups.AddToGroupAsync(Context.ConnectionId, tenantId);
                Console.WriteLine($"[SignalR] Connection {Context.ConnectionId} joined tenant group: {tenantId}");
            }

            await base.OnConnectedAsync();
        }

        public override async Task OnDisconnectedAsync(Exception? exception)
        {
            var httpContext = Context.GetHttpContext();
            var tenantId = httpContext?.Request.Query["tenantId"].ToString()
                           ?? httpContext?.Request.Headers["X-Tenant-Id"].ToString();

            if (!string.IsNullOrEmpty(tenantId))
            {
                await Groups.RemoveFromGroupAsync(Context.ConnectionId, tenantId);
                Console.WriteLine($"[SignalR] Connection {Context.ConnectionId} left tenant group: {tenantId}");
            }

            await base.OnDisconnectedAsync(exception);
        }
    }
}
