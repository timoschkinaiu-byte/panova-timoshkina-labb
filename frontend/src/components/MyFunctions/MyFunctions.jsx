import { useState, useEffect } from 'react';
import SearchModal from './SearchModal';
import FunctionModal from './FunctionModal';
import FunctionList from './FunctionList';
import functionService from '../../services/functionService';
import authService from '../../services/auth';
import notificationService from '../../services/notificationService';
import "../../App.css";

const MyFunctions = () => {
  const [functions, setFunctions] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchModalOpen, setSearchModalOpen] = useState(false);
  const [selectedFunction, setSelectedFunction] = useState(null);
  const [functionModalOpen, setFunctionModalOpen] = useState(false);
  const [searchParams, setSearchParams] = useState({
    searchType: 'name',
    searchValue: '',
    visibility: 'any',
    ownerId: null
  });
  const [currentUser, setCurrentUser] = useState(null);
  const [allUsers, setAllUsers] = useState([]);

  // Загрузка данных при монтировании
  useEffect(() => {
    loadInitialData();
  }, []);

  // Загрузка начальных данных
  const loadInitialData = async () => {
    setIsLoading(true);
    try {
      // Получаем текущего пользователя
      const user = authService.getCurrentUser();
      setCurrentUser(user);

      // Загружаем всех пользователей (для фильтра по владельцу)
      if (user?.role === 'ADMIN') {
        await loadAllUsers();
      }

      // Загружаем функции
      await loadFunctions();
    } catch (error) {
      console.error('Error loading initial data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // Загрузка всех пользователей (только для админов)
  const loadAllUsers = async () => {
    try {
      const response = await functionService.getAllUsers();
      if (response.data) {
        setAllUsers(response.data);
      }
    } catch (error) {
      console.error('Error loading users:', error);
    }
  };

  // Загрузка функций
  const loadFunctions = async (params = {}) => {
    setIsLoading(true);
    try {
      const response = await functionService.getMyFunctions(
        params.search || '',
        params.type || '',
        params.ownerId || null,
        params.isPublic !== undefined ? params.isPublic : null
      );

      if (response.data) {
        // Сортировка по дате создания (новые сверху)
        const sortedFunctions = response.data.sort((a, b) =>
          new Date(b.createdAt) - new Date(a.createdAt)
        );
        setFunctions(sortedFunctions);
      }
    } catch (error) {
      console.error('Error loading functions:', error);
      notificationService.error('Ошибка загрузки функций');
    } finally {
      setIsLoading(false);
    }
  };

  // Поиск функций с параметрами
  const handleSearch = async (params) => {
    setSearchParams(params);

    const searchParams = {
      search: params.searchValue || '',
      isPublic: params.visibility === 'any' ? null : (params.visibility === 'public'),
      ownerId: params.ownerId || null
    };

    if (params.searchType === 'id' && params.searchValue) {
      // Поиск по ID
      try {
        const response = await functionService.getFunctionById(parseInt(params.searchValue));
        if (response.data) {
          setFunctions([response.data]);
        } else {
          setFunctions([]);
          notificationService.warning('Функция с таким ID не найдена');
        }
      } catch (error) {
        setFunctions([]);
        notificationService.error('Ошибка при поиске по ID');
      }
    } else {
      // Поиск по названию с фильтрами
      await loadFunctions(searchParams);
    }

    setSearchModalOpen(false);
  };

  // Сброс фильтров
  const handleResetFilters = () => {
    setSearchParams({
      searchType: 'name',
      searchValue: '',
      visibility: 'any',
      ownerId: null
    });
    loadFunctions();
  };

  // Открыть функцию
  const handleOpenFunction = (func) => {
    setSelectedFunction(func);
    setFunctionModalOpen(true);
  };

  // Удалить функцию
  const handleDeleteFunction = async (functionId, functionName) => {
    const confirmed = window.confirm(`Вы уверены, что хотите удалить функцию "${functionName}"?`);
    if (!confirmed) return;

    try {
      await functionService.deleteFunction(functionId);
      // Обновляем список
      setFunctions(prev => prev.filter(f => f.functionId !== functionId));
      if (selectedFunction?.functionId === functionId) {
        setFunctionModalOpen(false);
      }
      notificationService.success(`Функция "${functionName}" успешно удалена`);
    } catch (error) {
      console.error('Error deleting function:', error);
      notificationService.error('Ошибка при удалении функции');
    }
  };

  // Обновить функцию
  const handleUpdateFunction = async (updatedData) => {
    try {
      await functionService.updateFunction(selectedFunction.functionId, updatedData);
      // Обновляем в списке
      setFunctions(prev => prev.map(f =>
        f.functionId === selectedFunction.functionId
          ? { ...f, ...updatedData }
          : f
      ));
      // Обновляем выбранную функцию
      setSelectedFunction(prev => ({ ...prev, ...updatedData }));
      notificationService.success('Функция успешно обновлена');
      return true;
    } catch (error) {
      console.error('Error updating function:', error);
      notificationService.error('Ошибка при обновлении функции');
      return false;
    }
  };

  // Экспорт функции
  const handleExportFunction = async (functionId, functionName) => {
    try {
      // Формируем безопасное имя файла
      const safeName = functionName
        .replace(/[^a-zA-Z0-9а-яА-ЯёЁ\s\-_]/g, '')
        .replace(/\s+/g, '_')
        .trim();

      const filename = `${safeName || 'function'}.json`;

      // Получаем файл через functionService
      const response = await functionService.exportFunction(functionId);

      // Создаем ссылку для скачивания
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();

      notificationService.success(`Функция "${functionName}" сохранена как ${filename}`);

    } catch (error) {
      console.error('Error exporting function:', error);
      notificationService.error('Ошибка при экспорте функции');
    }
  };

  return (
    <div className="my-functions-container">
      {/* Заголовок и кнопка поиска */}
      <div className="content-header">
        <h1 className="page-title">Мои функции</h1>
        <div className="header-actions">
          <button
            className="btn-primary"
            onClick={() => setSearchModalOpen(true)}
          >
            Поиск функции
          </button>
          <button
            className="btn-secondary"
            onClick={handleResetFilters}
            disabled={isLoading}
          >
            Обновить список
          </button>
        </div>
      </div>

      {/* Информация о текущем поиске */}
      {searchParams.searchValue || searchParams.visibility !== 'any' ? (
        <div className="search-info">
          <span>
            {searchParams.searchValue && `Поиск: ${searchParams.searchType === 'id' ? 'ID' : 'Название'} - "${searchParams.searchValue}"`}
            {searchParams.visibility !== 'any' && ` ${searchParams.searchValue ? '|' : ''} ${searchParams.visibility === 'public' ? 'Публичные' : 'Личные'}`}
          </span>
          <button
            className="btn-clear"
            onClick={handleResetFilters}
          >
            Сбросить фильтры
          </button>
        </div>
      ) : null}

      {/* Список функций */}
      <FunctionList
        functions={functions}
        isLoading={isLoading}
        onOpen={handleOpenFunction}
        onDelete={handleDeleteFunction}
      />

      {/* Модальное окно поиска */}
      <SearchModal
        isOpen={searchModalOpen}
        onClose={() => setSearchModalOpen(false)}
        onSearch={handleSearch}
        allUsers={allUsers}
        currentUser={currentUser}
      />

      {/* Модальное окно функции */}
      {selectedFunction && (
        <FunctionModal
          isOpen={functionModalOpen}
          onClose={() => setFunctionModalOpen(false)}
          function={selectedFunction}
          onDelete={handleDeleteFunction}
          onUpdate={handleUpdateFunction}
        />
      )}
    </div>
  );
};

export default MyFunctions;