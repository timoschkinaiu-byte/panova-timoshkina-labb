import { useState, useEffect } from 'react';
import "../../App.css";

const SearchModal = ({ isOpen, onClose, onSearch, allUsers = [], currentUser = null }) => {
  const [searchType, setSearchType] = useState('name');
  const [searchValue, setSearchValue] = useState('');
  const [visibility, setVisibility] = useState('any');
  const [selectedOwnerId, setSelectedOwnerId] = useState(null);
  const [availableOwners, setAvailableOwners] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Обновление доступных владельцев при изменении видимости
  useEffect(() => {
    let owners = [];

    if (visibility === 'any' || visibility === 'public') {
      // Для "любой" или "публичная" показываем всех пользователей
      if (allUsers && allUsers.length > 0) {
        owners = allUsers;
      } else if (currentUser) {
        owners = [currentUser];
      }
    } else if (visibility === 'private') {
      // Для "личная" показываем только текущего пользователя
      if (currentUser) {
        owners = [currentUser];
        setSelectedOwnerId(currentUser.userId);
      }
    }

    setAvailableOwners(owners || []);

    // Сбросить выбранного владельца, если он не входит в доступных
    if (selectedOwnerId && owners && !owners.some(owner => owner.userId === selectedOwnerId)) {
      setSelectedOwnerId(null);
    }
  }, [visibility, allUsers, currentUser]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!searchValue.trim() && searchType === 'id') {
      // Для поиска по ID значение обязательно
      return;
    }

    setIsSubmitting(true);

    const params = {
      searchType,
      searchValue: searchValue.trim(),
      visibility,
      ownerId: selectedOwnerId ? parseInt(selectedOwnerId) : null
    };

    try {
      await onSearch(params);
    } catch (error) {
      console.error('Search error:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = () => {
    setSearchValue('');
    setVisibility('any');
    setSelectedOwnerId(null);
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content search-modal">
        <div className="modal-header">
          <h3 className="modal-title">Поиск функции</h3>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Тип поиска */}
          <div className="form-group">
            <label className="form-label">Тип поиска</label>
            <div className="radio-group">
              <label className="radio-label">
                <input
                  type="radio"
                  name="searchType"
                  value="name"
                  checked={searchType === 'name'}
                  onChange={(e) => setSearchType(e.target.value)}
                  disabled={isSubmitting}
                />
                <span className="radio-text">По названию</span>
              </label>
              <label className="radio-label">
                <input
                  type="radio"
                  name="searchType"
                  value="id"
                  checked={searchType === 'id'}
                  onChange={(e) => setSearchType(e.target.value)}
                  disabled={isSubmitting}
                />
                <span className="radio-text">По ID</span>
              </label>
            </div>
          </div>

          {/* Поле ввода */}
          <div className="form-group">
            <label className="form-label">
              {searchType === 'id' ? 'ID функции' : 'Название функции (необязательно)'}
            </label>
            <input
              type={searchType === 'id' ? 'number' : 'text'}
              className="form-input"
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              placeholder={searchType === 'id' ? 'Введите ID...' : 'Введите название...'}
              disabled={isSubmitting}
              autoFocus
              required={searchType === 'id'}
              min={searchType === 'id' ? '1' : undefined}
            />
          </div>

          {/* Фильтр видимости */}
          <div className="form-group">
            <label className="form-label">Видимость</label>
            <div className="radio-group">
              <label className="radio-label">
                <input
                  type="radio"
                  name="visibility"
                  value="any"
                  checked={visibility === 'any'}
                  onChange={(e) => setVisibility(e.target.value)}
                  disabled={isSubmitting}
                />
                <span className="radio-text">Любая</span>
              </label>
              <label className="radio-label">
                <input
                  type="radio"
                  name="visibility"
                  value="public"
                  checked={visibility === 'public'}
                  onChange={(e) => setVisibility(e.target.value)}
                  disabled={isSubmitting}
                />
                <span className="radio-text">Публичные</span>
              </label>
              <label className="radio-label">
                <input
                  type="radio"
                  name="visibility"
                  value="private"
                  checked={visibility === 'private'}
                  onChange={(e) => setVisibility(e.target.value)}
                  disabled={isSubmitting}
                />
                <span className="radio-text">Личные</span>
              </label>
            </div>
          </div>

          {/* Выбор владельца (только для "любая" и "публичная") */}
          {(visibility === 'any' || visibility === 'public') && availableOwners.length > 0 && (
            <div className="form-group">
              <label className="form-label">Владелец</label>
              <select
                className="form-input"
                value={selectedOwnerId || ''}
                onChange={(e) => setSelectedOwnerId(e.target.value || null)}
                disabled={isSubmitting}
              >
                <option value="">Любой</option>
                {availableOwners.map(owner => (
                  <option key={owner.userId} value={owner.userId}>
                    {owner.username} {owner.userId === currentUser?.userId ? '(Вы)' : ''}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Кнопки */}
          <div className="modal-buttons">
            <button
              type="button"
              className="btn-secondary"
              onClick={handleReset}
              disabled={isSubmitting}
            >
              Сбросить
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Отмена
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={isSubmitting || (searchType === 'id' && !searchValue.trim())}
            >
              {isSubmitting ? 'Поиск...' : 'Искать'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SearchModal;