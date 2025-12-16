import { useState, useEffect } from 'react';
import PublicFunctionsList from './PublicFunctionsList';
import PublicSearchModal from './PublicSearchModal';
import PublicFunctionModal from './PublicFunctionModal';
import functionService from '../../services/functionService';
import notificationService from '../../services/notificationService';
import '../../App.css';

const PublicFunctions = () => {
  const [functions, setFunctions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showSearchModal, setShowSearchModal] = useState(false);
  const [showFunctionModal, setShowFunctionModal] = useState(false); // ← ДОБАВЬТЕ
  const [selectedFunction, setSelectedFunction] = useState(null); // ← ДОБАВЬТЕ
  const [searchParams, setSearchParams] = useState({});
  const [hasSearched, setHasSearched] = useState(false);

  // Загрузка публичных функций
  useEffect(() => {
    loadPublicFunctions();
  }, []);

  const loadPublicFunctions = async (params = {}) => {
    setIsLoading(true);
    try {
      // Загружаем только публичные функции
      const response = await functionService.getMyFunctions(
        params.search || '',
        params.type || '',
        null, // ownerId = null для всех пользователей
        true // isPublic = true
      );

      if (response.data) {
        setFunctions(response.data);
        setHasSearched(!!params.search || !!params.type);
      }
    } catch (error) {
      console.error('Error loading public functions:', error);
      notificationService.error('Ошибка загрузки публичных функций');
    } finally {
      setIsLoading(false);
    }
  };

  // Обработчик открытия функции
  const handleOpenFunction = (func) => {
    console.log('Opening function:', func); // ← для отладки
    setSelectedFunction(func);
    setShowFunctionModal(true);
  };

  const handleSearch = (searchData) => {
    setSearchParams(searchData);
    loadPublicFunctions(searchData);
    setShowSearchModal(false);
  };

  const handleClearSearch = () => {
    setSearchParams({});
    loadPublicFunctions();
    setHasSearched(false);
  };

  const handleRefresh = () => {
    loadPublicFunctions(searchParams);
  };

  return (
    <div className="public-functions-container">
      <div className="content-header">
        <div>
          <h1 className="page-title">Публичные функции</h1>
          <p className="page-description">
            Просмотр функций, которые другие пользователи сделали общедоступными
          </p>
        </div>

        <div className="header-actions">
          <button
            className="btn-secondary"
            onClick={() => setShowSearchModal(true)}
            disabled={isLoading}
          >
            Поиск
          </button>
          <button
            className="btn-secondary"
            onClick={handleRefresh}
            disabled={isLoading}
          >
            Обновить
          </button>
        </div>
      </div>

      {hasSearched && (
        <div className="search-info">
          <span>
            Поиск: {searchParams.search ? `"${searchParams.search}"` : ''}
            {searchParams.type && ` • Тип: ${searchParams.type}`}
          </span>
          <button
            className="btn-clear"
            onClick={handleClearSearch}
            disabled={isLoading}
          >
            Очистить поиск
          </button>
        </div>
      )}

      <PublicFunctionsList
        functions={functions}
        isLoading={isLoading}
        onOpen={handleOpenFunction} // ← ПЕРЕДАЕМ ФУНКЦИЮ!
      />

      {/* Модальное окно поиска */}
      <PublicSearchModal
        isOpen={showSearchModal}
        onClose={() => setShowSearchModal(false)}
        onSearch={handleSearch}
        isLoading={isLoading}
      />

      {/* Модальное окно функции */}
      <PublicFunctionModal
        isOpen={showFunctionModal}
        onClose={() => setShowFunctionModal(false)}
        function={selectedFunction}
      />
    </div>
  );
};

export default PublicFunctions;