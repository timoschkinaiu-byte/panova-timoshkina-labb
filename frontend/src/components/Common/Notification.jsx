import { useState, useEffect } from 'react';
import "../../App.css";

const Notification = ({ type, message, onClose }) => {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsVisible(false);
      setTimeout(() => {
        if (onClose) onClose();
      }, 300);
    }, 5000);

    return () => clearTimeout(timer);
  }, [onClose]);

  if (!isVisible) return null;

  const getTitle = () => {
    switch (type) {
      case 'success': return 'Успешно';
      case 'error': return 'Ошибка';
      case 'warning': return 'Предупреждение';
      case 'info': return 'Информация';
      default: return '';
    }
  };

  return (
    <div className={`notification ${type}`}>
      <div className="notification-header">
        <span className="notification-title">{getTitle()}</span>
        <button className="notification-close" onClick={() => setIsVisible(false)}>
          ×
        </button>
      </div>
      <div className="notification-message">
        {message}
      </div>
    </div>
  );
};

export const NotificationContainer = ({ notifications, removeNotification }) => {
  return (
    <div className="notification-container">
      {notifications.map(notification => (
        <Notification
          key={notification.id}
          type={notification.type}
          message={notification.message}
          onClose={() => removeNotification(notification.id)}
        />
      ))}
    </div>
  );
};

export default Notification;