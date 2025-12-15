import { useState } from 'react';
import functionService from '../../../services/functionService';
import GraphModal from '../../Common/GraphModal';
import "../../../App.css";

const FromFileTab = ({
  functionName,
  isPublic,
  isCreating,
  setIsCreating,
  onSuccess,
  onError
}) => {
  const [file, setFile] = useState(null);

  // Состояния для графика
  const [isGraphModalOpen, setIsGraphModalOpen] = useState(false);
  const [graphData, setGraphData] = useState([]);
  const [isGeneratingGraph, setIsGeneratingGraph] = useState(false);
  const [importedFunctionId, setImportedFunctionId] = useState(null);



  const handleFileSelect = (e) => {
    const selectedFile = e.target.files[0];
    if (selectedFile) {
      const fileName = selectedFile.name.toLowerCase();

      // Проверяем расширение
      const allowedExtensions = ['.txt', '.bin', '.ser', '.json', '.xml'];
      const isValidExtension = allowedExtensions.some(ext => fileName.endsWith(ext));

      if (!isValidExtension) {
        onError(`Неподдерживаемый формат. Разрешены: ${allowedExtensions.join(', ')}`);
        e.target.value = ''; // Сбросить выбор
        return;
      }

      // Проверяем размер (например, не больше 10MB)
      if (selectedFile.size > 10 * 1024 * 1024) {
        onError('Файл слишком большой (максимум 10MB)');
        e.target.value = '';
        return;
      }

      setFile(selectedFile);
      console.log('Выбран файл:', selectedFile.name, 'размер:', selectedFile.size, 'тип:', selectedFile.type);
    }
  };

  // Функция для предварительного просмотра графика импортированной функции
  const generatePreviewGraph = async () => {
    if (!file) {
      onError('Выберите файл для просмотра');
      return;
    }

    setIsGeneratingGraph(true);

    try {
      // Определяем формат по расширению
      const fileName = file.name.toLowerCase();
      let format = 'auto';

      if (fileName.endsWith('.txt')) format = 'text';
      else if (fileName.endsWith('.bin')) format = 'binary';
      else if (fileName.endsWith('.ser')) format = 'serialized';

      // Импортируем временную функцию
      const response = await functionService.importFunction(file, format);

      if (response.status === 201 && response.data?.functionId) {
        const functionId = response.data.functionId;
        setImportedFunctionId(functionId);

        // Получаем данные графика
        const graphResponse = await functionService.getGraphData(
          functionId,
          200,
          null,
          null
        );

        if (graphResponse.data && graphResponse.data.points) {
          setGraphData(graphResponse.data.points);
          setIsGraphModalOpen(true);
        } else {
          onError('Не удалось получить данные графика');
        }
      }
    } catch (error) {
      console.error('Error generating preview graph:', error);

      let errorMessage = 'Ошибка при построении графика';
      if (error.response?.status === 400) {
        errorMessage = 'Некорректный формат файла';
      } else if (error.response?.status === 401) {
        errorMessage = 'Недостаточно прав для импорта функции';
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (!error.response) {
        errorMessage = 'Сервер не отвечает. Проверьте подключение к API';
      }

      onError(errorMessage);
    } finally {
      setIsGeneratingGraph(false);
    }
  };


  const handleCreate = async () => {
    if (!file) {
      onError('Выберите файл для загрузки');
      return;
    }

    if (!functionName.trim()) {
      onError('Введите название функции');
      return;
    }

    setIsCreating(true);

    try {
      // Определяем формат по расширению
      const fileName = file.name.toLowerCase();
      let format = 'auto';

      if (fileName.endsWith('.txt')) {
        format = 'text';
      } else if (fileName.endsWith('.bin') || fileName.endsWith('.ser')) {
        format = 'binary';
      }



      console.log('Импортируем файл:', file.name, 'формат:', format);

      const response = await functionService.importFunction(file, format);

      if (response.status === 201) {
        onSuccess();
        setFile(null);
        // Сбросить input file
        document.getElementById('file-upload').value = '';
      }
    } catch (error) {
      console.error('Error importing function:', error);
      let message = 'Ошибка загрузки файла';

      if (error.response?.status === 400) {
        message = 'Некорректный формат файла';
      } else if (error.response?.status === 413) {
        message = 'Файл слишком большой';
      } else if (error.response?.data?.message) {
        message = error.response.data.message;
      } else if (error.message) {
        message = error.message;
      }

      onError(message);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCancel = () => {
    setFile(null);
    if (importedFunctionId) {
      // Удаляем временную функцию если она была создана
      functionService.deleteFunction(importedFunctionId).catch(console.error);
      setImportedFunctionId(null);
    }
  };

  // Удаление временной функции после закрытия графика
  const handleGraphModalClose = () => {
    if (importedFunctionId) {
      functionService.deleteFunction(importedFunctionId).catch(console.error);
      setImportedFunctionId(null);
    }
    setIsGraphModalOpen(false);
    setGraphData([]);
  };

  return (
    <div className="tab-content">
      <div className="form-section">
        <div className="form-group">
          <label className="form-label">Файл функции</label>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <input
              type="file"
              id="file-upload"
              onChange={handleFileSelect}
              disabled={isCreating}
              accept=".txt,.bin,.ser"
              style={{ display: 'none' }}
            />
            <label
              htmlFor="file-upload"
              className={`btn-secondary ${file ? 'active' : ''}`}
              style={{
                cursor: 'pointer',
                display: 'inline-block',
                padding: '10px 20px',
                borderRadius: '8px',
                border: file ? '2px solid var(--accent-color)' : '2px solid var(--input-border)'
              }}
            >
              {file ? 'Файл выбран' : 'Выбрать файл'}
            </label>
            <span style={{
              color: 'var(--text-secondary)',
              fontSize: '14px',
              maxWidth: '200px',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap'
            }}>
              {file ? file.name : 'Файл не выбран'}
            </span>
          </div>
          <p style={{
            fontSize: '12px',
            color: 'var(--text-secondary)',
            marginTop: '10px',
            padding: '8px',
            background: 'var(--accent-light)',
            borderRadius: '6px',
            border: '1px solid var(--accent-light)'
          }}>
            <strong>Поддерживаемые форматы:</strong>
            <br/>• Текстовый файл (.txt) - точки в формате "x y"
            <br/>• Бинарный файл (.bin) - двоичный формат

          </p>

          {/* Кнопка предварительного просмотра */}
          {file && (
            <div style={{ marginTop: '20px', textAlign: 'center' }}>
              <button
                className="btn-secondary"
                onClick={generatePreviewGraph}
                disabled={isGeneratingGraph || isCreating}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  margin: '0 auto',
                  padding: '10px 20px'
                }}
              >
                {isGeneratingGraph ? (
                  <>
                    <div className="loading-spinner" style={{ width: '16px', height: '16px' }}></div>
                    Построение графика...
                  </>
                ) : (
                  <>
                    📊 Предварительный просмотр графика
                  </>
                )}
              </button>
              <p style={{
                fontSize: '12px',
                color: 'var(--text-secondary)',
                marginTop: '5px'
              }}>
                Загрузит функцию временно и покажет её график
              </p>
            </div>
          )}
        </div>
      </div>

      {file && (
        <div className="action-buttons">
          <button
            className="btn-secondary"
            onClick={handleCancel}
            disabled={isCreating}
          >
            Отмена
          </button>
          <button
            className="btn-primary"
            onClick={handleCreate}
            disabled={isCreating}
          >
            {isCreating ? 'Загрузка...' : 'Загрузить функцию'}
          </button>
        </div>
      )}

      {/* Модальное окно графика */}
      <GraphModal
        isOpen={isGraphModalOpen}
        onClose={handleGraphModalClose}
        points={graphData}
        title={file ? `Функция из файла: ${file.name}` : "Импортированная функция"}
      />
    </div>
  );
};

export default FromFileTab;