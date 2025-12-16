import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../../services/auth';
import './AuthPage.css';

const AuthPage = () => {
  const [activeTab, setActiveTab] = useState('login');
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    confirmPassword: ''
  });
  const [errors, setErrors] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [serverMessage, setServerMessage] = useState({ type: '', text: '' });
  const navigate = useNavigate();

  // Проверяем, авторизован ли пользователь
/*  useEffect(() => {
    const checkAuth = async () => {
      try {
        const user = authService.getCurrentUser();
        if (user) {
          navigate('/dashboard');
        }
      } catch (error) {
        // Не авторизован - остаёмся на странице
      }
    };
    checkAuth();
  }, [navigate]);
*/
  // Валидация форм
  const validateForm = () => {
    const newErrors = {};

    // Общие проверки
    if (!formData.username.trim()) {
      newErrors.username = 'Имя пользователя обязательно';
    } else if (formData.username.length < 3) {
      newErrors.username = 'Минимум 3 символа';
    } else if (formData.username.length > 20) {
      newErrors.username = 'Максимум 20 символов';
    } else if (!/^[a-zA-Z0-9_]+$/.test(formData.username)) {
      newErrors.username = 'Только латинские буквы, цифры и нижнее подчеркивание';
    }

    // Проверка пароля
    if (!formData.password) {
      newErrors.password = 'Пароль обязателен';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Минимум 6 символов';
    } else if (formData.password.includes(' ')) {
      newErrors.password = 'Пароль не может содержать пробелы';
    }

    // Проверки для регистрации
    if (activeTab === 'register') {
      if (!formData.confirmPassword) {
        newErrors.confirmPassword = 'Подтвердите пароль';
      } else if (formData.password !== formData.confirmPassword) {
        newErrors.confirmPassword = 'Пароли не совпадают';
      } else if (formData.confirmPassword.includes(' ')) {
        newErrors.confirmPassword = 'Пароль не может содержать пробелы';
      }
    }

    return newErrors;
  };

  // Обработчик отправки формы
  const handleSubmit = async (e) => {
    e.preventDefault();
    setServerMessage({ type: '', text: '' });

    const validationErrors = validateForm();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setIsLoading(true);
    setErrors({});

    try {
      let result;
      if (activeTab === 'login') {
        result = await authService.login(formData.username, formData.password);
      } else {
        result = await authService.register(formData.username, formData.password);
      }

      if (result.success) {
        setServerMessage({
          type: 'success',
          text: activeTab === 'login' ? 'Вход выполнен!' : 'Регистрация успешна!'
        });

        // Редирект после небольшой задержки
        setTimeout(() => {
          navigate('/dashboard');
        }, 1500);
      } else {
        setServerMessage({ type: 'error', text: result.error });
      }
    } catch (error) {
      setServerMessage({
        type: 'error',
        text: 'Произошла непредвиденная ошибка'
      });
    } finally {
      setIsLoading(false);
    }
  };

  // Обработчик изменения полей
  const handleInputChange = (e) => {
    const { name, value } = e.target;

    // Для поля пароля - удаляем пробелы в начале и конце
    const processedValue = (name === 'password' || name === 'confirmPassword')
      ? value.trim()
      : value;

    setFormData(prev => ({ ...prev, [name]: processedValue }));

    // Очищаем ошибку при вводе
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  // Сброс формы при смене вкладки
  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setFormData({ username: '', password: '', confirmPassword: '' });
    setErrors({});
    setServerMessage({ type: '', text: '' });
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        {/* Вкладки */}
        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${activeTab === 'login' ? 'active' : ''}`}
            onClick={() => handleTabChange('login')}
            disabled={isLoading}
          >
            Вход
          </button>
          <button
            type="button"
            className={`auth-tab ${activeTab === 'register' ? 'active' : ''}`}
            onClick={() => handleTabChange('register')}
            disabled={isLoading}
          >
            Регистрация
          </button>
        </div>

        {/* Сообщения сервера */}
        {serverMessage.text && (
          <div className={`server-message ${serverMessage.type}`}>
            {serverMessage.text}
          </div>
        )}

        {/* Форма */}
        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          {/* Поле имени пользователя */}
          <div className="form-group">
            <label htmlFor="username" className="form-label">
              Имя пользователя
            </label>
            <input
              id="username"
              type="text"
              name="username"
              value={formData.username}
              onChange={handleInputChange}
              className={`form-input ${errors.username ? 'error' : ''}`}
              disabled={isLoading}
              placeholder="Введите имя пользователя"
              autoComplete="username"
              pattern="[a-zA-Z0-9_]+"
              //title="Только латинские буквы, цифры и нижнее подчеркивание"
            />
            {errors.username && (
              <span className="error-message">{errors.username}</span>
            )}
          </div>

          {/* Поле пароля */}
          <div className="form-group">
            <label htmlFor="password" className="form-label">
              Пароль <span>(минимум 6 символов)</span>
            </label>
            <input
              id="password"
              type="password"
              name="password"
              value={formData.password}
              onChange={handleInputChange}
              className={`form-input ${errors.password ? 'error' : ''}`}
              disabled={isLoading}
              placeholder="Введите пароль"
              autoComplete="current-password"
              minLength="6"
            />
            {errors.password && (
              <span className="error-message">{errors.password}</span>
            )}
          </div>

          {/* Подтверждение пароля (только для регистрации) */}
          {activeTab === 'register' && (
            <div className="form-group">
              <label htmlFor="confirmPassword" className="form-label">
                Подтвердите пароль
              </label>
              <input
                id="confirmPassword"
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleInputChange}
                className={`form-input ${errors.confirmPassword ? 'error' : ''}`}
                disabled={isLoading}
                placeholder="Повторите пароль"
                autoComplete="new-password"
                minLength="6"
              />
              {errors.confirmPassword && (
                <span className="error-message">{errors.confirmPassword}</span>
              )}
            </div>
          )}

          {/* Кнопка отправки */}
          <button
            type="submit"
            className="submit-button"
            disabled={isLoading}
          >
            {isLoading ? (
              <span className="loading-text">Обработка...</span>
            ) : activeTab === 'login' ? (
              'Войти в систему'
            ) : (
              'Создать аккаунт'
            )}
          </button>
        </form>

        {/* Дополнительная информация */}
        <div className="auth-footer">
          {activeTab === 'login' ? (
            <p className="footer-text">
              Нет аккаунта?{' '}
              <button
                type="button"
                className="footer-link"
                onClick={() => handleTabChange('register')}
                disabled={isLoading}
              >
                Зарегистрироваться
              </button>
            </p>
          ) : (
            <p className="footer-text">
              Уже есть аккаунт?{' '}
              <button
                type="button"
                className="footer-link"
                onClick={() => handleTabChange('login')}
                disabled={isLoading}
              >
                Войти
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default AuthPage;