
import "../../App.css";

const ErrorModal = ({ isOpen, error, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h3 className="modal-title">Ошибка</h3>
          <p className="modal-description">Произошла ошибка при выполнении операции</p>
        </div>

        <div className="form-group">
          <div className="server-message error">
            {error || 'Неизвестная ошибка'}
          </div>
        </div>

        <div className="modal-buttons">
          <button className="btn-secondary" onClick={onClose}>
            Закрыть
          </button>
        </div>
      </div>
    </div>
  );
};

export default ErrorModal;