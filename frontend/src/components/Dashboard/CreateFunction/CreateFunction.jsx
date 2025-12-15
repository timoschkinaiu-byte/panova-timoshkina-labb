import { useState } from 'react';
import FromArraysTab from './FromArraysTab';
import FromFunctionTab from './FromFunctionTab';
import FromFileTab from './FromFileTab';
import CompositeTab from './CompositeTab';
import ErrorModal from '../../Layout/ErrorModal';
import notificationService from '../../../services/notificationService';
import "../../../App.css";

const CreateFunction = () => {
  const [activeTab, setActiveTab] = useState('from-function');
  const [functionName, setFunctionName] = useState('');
  const [isPublic, setIsPublic] = useState(false);
  const [error, setError] = useState(null);
  const [isErrorModalOpen, setIsErrorModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const handleSuccess = () => {
    // Очищаем форму
    setFunctionName('');
    setIsPublic(false);

    // Показываем уведомление
    notificationService.success('Функция успешно создана');
  };

  const handleError = (errorMessage) => {
    setError(errorMessage);
    setIsErrorModalOpen(true);
  };

  const renderTabContent = () => {
    const commonProps = {
      functionName,
      setFunctionName,
      isPublic,
      setIsPublic,
      isCreating,
      setIsCreating,
      onSuccess: handleSuccess,
      onError: handleError
    };

    switch (activeTab) {
      case 'from-function':
        return <FromFunctionTab {...commonProps} />;
      case 'from-arrays':
        return <FromArraysTab {...commonProps} />;
      case 'from-file':
        return <FromFileTab {...commonProps} />;
      case 'composite':
        return <CompositeTab {...commonProps} />;
      default:
        return <FromFunctionTab {...commonProps} />;
    }
  };

  return (
    <>
      <div className="content-header">
        <h1 className="page-title">Создать функцию</h1>

      </div>


      {/* Верхняя панель с названием и публичностью */}
      <div className="form-section">
        <div className="form-row" style={{ display: 'flex', gap: '20px', marginBottom: '30px' }}>
          <div className="form-group" style={{ flex: '0 0 300px'  }}>
            <label className="form-label">Название функции</label>
            <input
              type="text"
              className="form-input"
              value={functionName}
              onChange={(e) => setFunctionName(e.target.value)}
              placeholder="Введите название функции"
              disabled={isCreating}
            />
          </div>

          <div className="form-group" style={{ marginTop: '24px' }}>
            <label className="form-label">Публичность</label>
            <div className="switch-container">
              <label className="switch">
                <input
                  type="checkbox"
                  checked={isPublic}
                  onChange={(e) => setIsPublic(e.target.checked)}
                  disabled={isCreating}
                />
                <span className="slider"></span>
              </label>
              <span className="switch-label">Сделать публичной</span>
            </div>
          </div>
        </div>
      </div>

      {/* Вкладки выбора метода */}
      <div className="tabs-container">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'from-function' ? 'active' : ''}`}
            onClick={() => setActiveTab('from-function')}
            disabled={isCreating}
          >
            Создать из функции
          </button>
          <button
            className={`tab ${activeTab === 'from-arrays' ? 'active' : ''}`}
            onClick={() => setActiveTab('from-arrays')}
            disabled={isCreating}
          >
            Создать из массивов
          </button>
          <button
            className={`tab ${activeTab === 'from-file' ? 'active' : ''}`}
            onClick={() => setActiveTab('from-file')}
            disabled={isCreating}
          >
            Загрузить из файла
          </button>
          <button
            className={`tab ${activeTab === 'composite' ? 'active' : ''}`}
            onClick={() => setActiveTab('composite')}
            disabled={isCreating}
          >
            Создать сложную функцию
          </button>
        </div>
      </div>

      {/* Динамическое содержимое вкладки */}
      {renderTabContent()}

      {/* Модальное окно ошибок */}
      <ErrorModal
        isOpen={isErrorModalOpen}
        error={error}
        onClose={() => setIsErrorModalOpen(false)}
      />
    </>
  );
};

export default CreateFunction;