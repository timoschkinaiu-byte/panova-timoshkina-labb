import { useState } from 'react';
import "../../App.css";

const FunctionList = ({ functions, isLoading, onOpen, onDelete }) => {
  const [hoveredRow, setHoveredRow] = useState(null);
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    functionId: null,
    functionName: ''
  });

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

  // Открытие модального окна удаления
  const openDeleteModal = (functionId, functionName) => {
    setDeleteModal({
      isOpen: true,
      functionId,
      functionName
    });
  };

  // Закрытие модального окна
  const closeDeleteModal = () => {
    setDeleteModal({
      isOpen: false,
      functionId: null,
      functionName: ''
    });
  };

  // Подтверждение удаления
  const confirmDelete = () => {
    if (deleteModal.functionId) {
      onDelete(deleteModal.functionId, deleteModal.functionName);
      closeDeleteModal();
    }
  };

  if (isLoading) {
    return (
      <>
        <div className="function-list loading">
          <div className="loading-spinner"></div>
          <p>Загрузка функций...</p>
        </div>
      </>
    );
  }

  if (functions.length === 0) {
    return (
      <>
        <div className="function-list empty">
          <p>Функции не найдены</p>
          <small>Создайте новую функцию или измените параметры поиска</small>
        </div>
      </>
    );
  }

  return (
    <>
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
                  onClick={() => openDeleteModal(func.functionId, func.functionName)}
                  title="Удалить функцию"
                >
                  Удалить
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Модальное окно подтверждения удаления */}
      {deleteModal.isOpen && (
        <div className="modal-overlay" onClick={closeDeleteModal}>
          <div className="modal-content delete-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Подтверждение удаления</h3>
              <p className="modal-description">
                Вы уверены, что хотите удалить эту функцию?
              </p>
            </div>

            <div className="delete-info">
              <div className="delete-item">
                <span className="delete-label">Название:</span>
                <span className="delete-value">{deleteModal.functionName}</span>
              </div>
              <div className="delete-item">
                <span className="delete-label">ID:</span>
                <span className="delete-value">#{deleteModal.functionId}</span>
              </div>
              <div className="warning-message">
                ⚠️ Это действие нельзя отменить. Все точки функции будут удалены безвозвратно.
              </div>
            </div>

            <div className="modal-buttons">
              <button className="btn-secondary" onClick={closeDeleteModal}>
                Отмена
              </button>
              <button className="btn-danger" onClick={confirmDelete}>
                Удалить функцию
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default FunctionList;