import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import AuthPage from './components/Auth/AuthPage';
import Dashboard from './components/Dashboard/Dashboard';
import PrivateRoute from './components/Routing/PrivateRoute';
import authService from './services/auth';
import './App.css';


// временно
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

const App = () => {
  const [isDarkMode, setIsDarkMode] = useState(true);

  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'light') {
      setIsDarkMode(false);
      document.body.classList.add('light-theme');
    }
  }, []);

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
          <ThemeToggle isDarkMode={isDarkMode} toggleTheme={toggleTheme} />  {/* кнопка смены темы */}

        <Routes>
          {/* Публичные маршруты */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<AuthPage />} />

          {/* Защищённые маршруты */}
          <Route
            path="/dashboard/*"
            element={
              <PrivateRoute>
                <Dashboard />
              </PrivateRoute>
            }
          />

          {/* Редирект для неизвестных путей */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </div>
    </Router>
  );
};

export default App;