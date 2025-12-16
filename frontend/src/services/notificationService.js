let notifications = [];
let subscribers = [];

const notificationService = {
  success(message) {
    this.addNotification('success', message);
  },

  error(message) {
    this.addNotification('error', message);
  },

  warning(message) {
    this.addNotification('warning', message);
  },

  info(message) {
    this.addNotification('info', message);
  },

  addNotification(type, message) {
    const id = Date.now();
    const notification = { id, type, message };
    notifications = [...notifications, notification];
    this.notifySubscribers();

    // Автоматическое удаление через 5 секунд
    setTimeout(() => {
      this.removeNotification(id);
    }, 5000);
  },

  removeNotification(id) {
    notifications = notifications.filter(n => n.id !== id);
    this.notifySubscribers();
  },

  subscribe(callback) {
    subscribers.push(callback);
    callback(notifications);

    return () => {
      subscribers = subscribers.filter(sub => sub !== callback);
    };
  },

  notifySubscribers() {
    subscribers.forEach(callback => callback(notifications));
  },

  getNotifications() {
    return notifications;
  }
};

export default notificationService;