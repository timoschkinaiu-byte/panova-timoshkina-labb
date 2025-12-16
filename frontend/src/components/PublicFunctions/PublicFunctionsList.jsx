import { useState } from 'react';
import authService from '../../services/auth';
import "../../App.css";

const PublicFunctionsList = ({ functions, isLoading, onOpen }) => {
  const [hoveredRow, setHoveredRow] = useState(null);

  const currentUser = authService.getCurrentUser();

  // Форматирование даты
  const formatDateTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('ru-RU', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Определяем, является ли функция владельцем текущим пользователем
  const isMyFunction = (func) => {
    return currentUser && func.ownerId === currentUser.userId;
  };

  // Получаем отображаемое имя владельца
  const getOwnerDisplayName = (func) => {
    // Пробуем получить логин из различных полей
    if (func.ownerUsername) {
      return func.ownerUsername;
    }
    if (func.ownerName) {
      return func.ownerName;
    }
    // Если это текущий пользователь
    if (isMyFunction(func)) {
      return currentUser.username || 'Вы';
    }
    // Если нет данных, показываем ID
    return `ID:${func.ownerId}`;
  };

  if (isLoading) {
    return (
      <div className="function-list loading">
        <div className="loading-spinner"></div>
        <p>Загрузка публичных функций...</p>
      </div>
    );
  }

  if (functions.length === 0) {
    return (
      <div className="function-list empty">
        <p>Публичные функции не найдены</p>
        <small>Здесь будут отображаться функции, которые другие пользователи сделали публичными</small>
      </div>
    );
  }

  return (
    <div className="function-list">
      <div className="functions-table">
        {/* Заголовок таблицы */}
        <div className="table-header">
          <div className="table-cell name">Название</div>
          <div className="table-cell id">ID</div>
          <div className="table-cell owner">Владелец</div>
          <div className="table-cell date">Дата создания</div>
          <div className="table-cell actions">Действия</div>
        </div>

        {/* Строки функций */}
        {functions.map(func => {
          const ownerName = getOwnerDisplayName(func);
          const isMine = isMyFunction(func);

          return (
            <div
              key={func.functionId}
              className="function-row"
              onMouseEnter={() => setHoveredRow(func.functionId)}
              onMouseLeave={() => setHoveredRow(null)}
            >
              <div className="table-cell name">
                <span className="function-name">{func.functionName}</span>
                {func.isPublic && <span className="public-badge" title="Публичная функция">🌐</span>}
                {isMine && <span className="my-badge" title="Ваша функция">👤</span>}
              </div>

              <div className="table-cell id">
                <span className="function-id">#{func.functionId}</span>
              </div>

              <div className="table-cell owner">
                <span className="owner-name">
                  {ownerName}
                </span>
              </div>



              <div className="table-cell date">
                <span className="function-date">{formatDateTime(func.createdAt)}</span>
              </div>



              <div className="table-cell actions">
                <button
                  className={`btn-action open ${hoveredRow === func.functionId ? 'hovered' : ''}`}
                  onClick={() => onOpen(func)}
                  title="Открыть функцию"
                >
                  Открыть
                </button>

                {isMine && (
                  <span className="edit-hint" title="Вы можете редактировать свою функцию в разделе 'Мои функции'">
                    ✏️
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default PublicFunctionsList;