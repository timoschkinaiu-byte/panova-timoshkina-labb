
import "../../App.css";

const Sidebar = ({ activeSection, setActiveSection }) => {
  const menuItems = [
    { id: 'create', label: 'Создать функцию' },
    { id: 'my-functions', label: 'Мои функции' },
    { id: 'operations', label: 'Операции с функциями' },
    { id: 'differentiation', label: 'Дифференцирование' },
    { id: 'integration', label: 'Интегрирование' },
    { id: 'public-functions', label: 'Публичные функции' },
    { id: 'settings', label: 'Настройки' }
  ];

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <h2 className="sidebar-title">Математические функции</h2>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map(item => (
          <button
            key={item.id}
            className={`nav-item ${activeSection === item.id ? 'active' : ''}`}
            onClick={() => setActiveSection(item.id)}
          >
            {item.label}
          </button>
        ))}
      </nav>
    </div>
  );
};

export default Sidebar;