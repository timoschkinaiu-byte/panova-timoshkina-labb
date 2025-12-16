import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../../services/auth';
import userService from '../../services/userService';
import notificationService from '../../services/notificationService';
import '../../App.css';

const Settings = () => {
  const [user, setUser] = useState(null);
  const [isDarkMode, setIsDarkMode] = useState(true);
  const [factoryType, setFactoryType] = useState('ARRAY');
  const [isEditingUsername, setIsEditingUsername] = useState(false);
  const [isEditingPassword, setIsEditingPassword] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const navigate = useNavigate();

  // Загрузка данных пользователя и настроек
  useEffect(() => {
    loadUserData();
    loadSettings();
  }, []);

  const loadUserData = () => {
    const currentUser = authService.getCurrentUser();
    console.log('Current user:', currentUser);
    if (currentUser) {
      setUser(currentUser);
      setNewUsername(currentUser.username || '');
    }
  };

  const loadSettings = () => {
    // Загрузка темы
    const savedTheme = localStorage.getItem('theme');
    const isDark = savedTheme !== 'light';
    setIsDarkMode(isDark);

    // Загрузка фабрики
    const savedFactory = localStorage.getItem('factoryType') || 'ARRAY';
    setFactoryType(savedFactory);
  };

  // Переключение темы
  const toggleTheme = () => {
    const newTheme = !isDarkMode;
    setIsDarkMode(newTheme);

    if (newTheme) {
      document.body.classList.remove('light-theme');
      localStorage.setItem('theme', 'dark');
    } else {
      document.body.classList.add('light-theme');
      localStorage.setItem('theme', 'light');
    }

    notificationService.success(`Тема изменена на ${newTheme ? 'тёмную' : 'светлую'}`);
  };

  // Изменение фабрики
  const handleFactoryChange = (type) => {
    setFactoryType(type);
    localStorage.setItem('factoryType', type);
    notificationService.success(`Фабрика изменена на ${type === 'ARRAY' ? 'Array' : 'LinkedList'}`);
  };




  // Выход из аккаунта
  const handleLogout = () => {
    authService.logout();
    notificationService.success('Вы успешно вышли из системы');
    navigate('/login');
  };

  // Удаление аккаунта
  const handleDeleteAccount = async () => {
    setIsLoading(true);
    try {
      const response = await userService.deleteUser(user.userId);

      if (response.status === 204) {
        authService.logout();
        notificationService.success('Аккаунт успешно удален');
        navigate('/login');
      }
    } catch (error) {
      console.error('Error deleting account:', error);
      notificationService.error('Ошибка удаления аккаунта');
    } finally {
      setIsLoading(false);
      setShowDeleteConfirm(false);
    }
  };

  if (!user) {
    return (
      <div className="settings-container">
        <div className="content-header">
          <h1 className="page-title">Настройки</h1>
        </div>
        <div className="settings-loading">
          <p>Загрузка данных пользователя...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="settings-container">
      <div className="content-header">
        <h1 className="page-title">Настройки</h1>

      </div>

      <div className="settings-grid">
        {/* Информация о пользователе */}
        <div className="settings-card">
          <h3 className="settings-card-title">Информация о пользователе</h3>
          <div className="user-info">
            <div className="info-row">
              <span className="info-label">ID:</span>
              <span className="info-value">{user.userId}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Логин:</span>
              <span className="info-value">{user.username}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Роль:</span>
              <span className={`role-badge ${user.role?.toLowerCase()}`}>
                {user.role || 'USER'}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">Дата регистрации:</span>
              <span className="info-value">
                {user.createdAt ? new Date(user.createdAt).toLocaleDateString('ru-RU') : '—'}
              </span>
            </div>
          </div>



        </div>

        {/* Настройки приложения */}
        <div className="settings-card">
          <h3 className="settings-card-title">Настройки приложения</h3>

          {/* Тема */}
          <div className="setting-item">
            <div className="setting-info">
              <h4>Тема интерфейса</h4>
              <p>Выберите светлую или тёмную тему</p>
            </div>
            <div className="setting-control">
              <div className="theme-toggle-large">
                <span className={`theme-option ${!isDarkMode ? 'active' : ''}`}>
                  Светлая
                </span>
                <label className="switch">
                  <input
                    type="checkbox"
                    checked={isDarkMode}
                    onChange={toggleTheme}
                    disabled={isLoading}
                  />
                  <span className="slider"></span>
                </label>
                <span className={`theme-option ${isDarkMode ? 'active' : ''}`}>
                  Тёмная
                </span>
              </div>
            </div>
          </div>

          {/* Фабрика */}
          <div className="setting-item">
            <div className="setting-info">
              <h4>Фабрика функций</h4>
              <p>Выберите тип внутреннего представления табулированных функций</p>
            </div>
            <div className="setting-control">
              <div className="factory-buttons">
                <button
                  className={`factory-button ${factoryType === 'ARRAY' ? 'active' : ''}`}
                  onClick={() => handleFactoryChange('ARRAY')}
                  disabled={isLoading}
                >
                  Array
                  <small>Массив (по умолчанию)</small>
                </button>
                <button
                  className={`factory-button ${factoryType === 'LINKED_LIST' ? 'active' : ''}`}
                  onClick={() => handleFactoryChange('LINKED_LIST')}
                  disabled={isLoading}
                >
                  LinkedList
                  <small>Связный список</small>
                </button>
              </div>
            </div>
          </div>

          {/* Опасные действия */}
          <div className="settings-card danger-zone">
            <h3 className="settings-card-title danger">Опасная зона</h3>

            <div className="danger-actions">
              <div className="danger-action">
                <div className="danger-info">
                  <h4>Выйти из аккаунта</h4>
                  <p>Завершить текущий сеанс работы</p>
                </div>
                <button
                  className="btn-secondary btn-danger"
                  onClick={() => setShowLogoutConfirm(true)}
                  disabled={isLoading}
                >
                  Выйти
                </button>
              </div>

              <div className="danger-action">
                <div className="danger-info">
                  <h4>Удалить аккаунт</h4>
                  <p>Безвозвратное удаление аккаунта и всех данных</p>
                </div>
                <button
                  className="btn-secondary btn-danger"
                  onClick={() => setShowDeleteConfirm(true)}
                  disabled={isLoading}
                >
                  Удалить аккаунт
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Модальное окно подтверждения выхода */}
      {showLogoutConfirm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">Подтверждение выхода</h3>
              <p className="modal-description">Вы уверены, что хотите выйти из системы?</p>
            </div>
            <div className="modal-buttons">
              <button
                className="btn-secondary"
                onClick={() => setShowLogoutConfirm(false)}
                disabled={isLoading}
              >
                Отмена
              </button>
              <button
                className="btn-primary"
                onClick={handleLogout}
                disabled={isLoading}
              >
                Выйти
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Модальное окно подтверждения удаления */}
      {showDeleteConfirm && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title danger">Удаление аккаунта</h3>
              <p className="modal-description">
                Вы уверены, что хотите удалить свой аккаунт?<br />
                <strong>Это действие нельзя отменить!</strong><br />
                Все ваши функции и данные будут удалены.
              </p>
            </div>
            <div className="modal-buttons">
              <button
                className="btn-secondary"
                onClick={() => setShowDeleteConfirm(false)}
                disabled={isLoading}
              >
                Отмена
              </button>
              <button
                className="btn-primary btn-danger"
                onClick={handleDeleteAccount}
                disabled={isLoading}
              >
                Удалить аккаунт
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Settings;