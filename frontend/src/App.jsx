import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import AuthPage from './components/Auth/AuthPage';
import './App.css';

// Компонент для переключения темы
const ThemeToggle = ({ isDarkMode, toggleTheme }) => {
  return (
    <button
      className="theme-toggle"
      onClick={toggleTheme}
      title={isDarkMode ? "Переключить на светлую тему" : "Переключить на темную тему"}
    >
      {isDarkMode ? '☀️' : '🌙'}
    </button>
  );
};

function App() {
  const [isDarkMode, setIsDarkMode] = useState(true);

  // Загружаем тему из localStorage при загрузке
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'light') {
      setIsDarkMode(false);
      document.body.classList.add('light-theme');
    }
  }, []);

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
  };

  return (
    <Router>
      <div className="app">
        {/* Переключатель темы */}
        <ThemeToggle isDarkMode={isDarkMode} toggleTheme={toggleTheme} />

        <Routes>
          {/* Главная страница - авторизация */}
          <Route path="/" element={<AuthPage />} />
          <Route path="/login" element={<AuthPage />} />

          {/* Заглушки для будущих страниц */}
          <Route path="/dashboard" element={<div>Главная страница (будет позже)</div>} />
          <Route path="/functions" element={<div>Мои функции (будет позже)</div>} />

          {/* Редирект на авторизацию для неизвестных путей */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;