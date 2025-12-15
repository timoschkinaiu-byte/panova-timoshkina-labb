import { useState, useEffect } from 'react';
import Sidebar from '../Layout/Sidebar';
import CreateFunction from './CreateFunction/CreateFunction';
import MyFunctions from '../MyFunctions/MyFunctions';
import Operations from '../Operations/Operations';
import Differentiation from '../Differentiation/Differentiation';
import Integration from '../Integration/Integration';
import PublicFunctions from '../PublicFunctions/PublicFunctions';
import Settings from '../Settings/Settings';
import { NotificationContainer } from '../Common/Notification';
import notificationService from '../../services/notificationService';
import "../../App.css";



const Dashboard = () => {
  const [activeSection, setActiveSection] = useState('create');
  const [notifications, setNotifications] = useState([]);

  // Подписка на уведомления
  useEffect(() => {
    const unsubscribe = notificationService.subscribe(setNotifications);
    return unsubscribe;
  }, []);

  // Сохранение выбранного раздела в localStorage
  useEffect(() => {
    const savedSection = localStorage.getItem('activeSection');
    if (savedSection && ['create', 'my-functions', 'operations', 'differentiation', 'integration', 'public-functions', 'settings'].includes(savedSection)) {
      setActiveSection(savedSection);
    }
  }, []);

  const handleSectionChange = (section) => {
    setActiveSection(section);
    localStorage.setItem('activeSection', section);
  };

  const removeNotification = (id) => {
    notificationService.removeNotification(id);
  };

  const renderContent = () => {
    switch (activeSection) {
      case 'create':
        return <CreateFunction />;
      case 'my-functions':
        return <MyFunctions />;
      case 'operations':
        return <Operations />;
      case 'differentiation':
        return <Differentiation />;
      case 'integration':
        return <Integration />;
      case 'public-functions':
        return <PublicFunctions />;
      case 'settings':
        return <Settings />;
      default:
        return <CreateFunction />;
    }
  };

  return (
    <div className="dashboard-container">
      <Sidebar
        activeSection={activeSection}
        setActiveSection={handleSectionChange}
      />

      <div className="main-content">
        {renderContent()}
      </div>

      <NotificationContainer
        notifications={notifications}
        removeNotification={removeNotification}
      />
    </div>
  );
};

export default Dashboard;



