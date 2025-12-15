
import { useState } from 'react';
import "../../App.css";

const FunctionList = ({ functions, isLoading, onOpen, onDelete }) => {
  const [hoveredRow, setHoveredRow] = useState(null);

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

  if (isLoading) {
    return (
      <div className="function-list loading">
        <div className="loading-spinner"></div>
        <p>Загрузка функций...</p>
      </div>
    );
  }

  if (functions.length === 0) {
    return (
      <div className="function-list empty">
        <p>Функции не найдены</p>
        <small>Создайте новую функцию или измените параметры поиска</small>
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
        {functions.map(func => (
          <div
            key={func.functionId}
            className="function-row"
            onMouseEnter={() => setHoveredRow(func.functionId)}
            onMouseLeave={() => setHoveredRow(null)}
          >
            <div className="table-cell name">
              <span className="function-name">{func.functionName}</span>
              {func.isPublic && <span className="public-badge" title="Публичная функция">🌐</span>}
            </div>
            <div className="table-cell id">
              <span className="function-id">#{func.functionId}</span>
            </div>
            <div className="table-cell owner">
              <span className="owner-name">{func.ownerId === func.currentUser?.userId ? 'Вы' : `Пользователь ${func.ownerId}`}</span>
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
              <button
                className={`btn-action delete ${hoveredRow === func.functionId ? 'hovered' : ''}`}
                onClick={() => onDelete(func.functionId, func.functionName)}
                title="Удалить функцию"
              >
                Удалить
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default FunctionList;