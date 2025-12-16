import API from './api';

const userService = {
  // Обновление пользователя

  async updateUser(userId, updateData) {
      return API.put(`/users/${userId}`, updateData, {
        params: { username: updateData.username }
      });
    },


  // Удаление пользователя
  async deleteUser(userId) {
    return API.delete(`/users/${userId}`);
  },

  // Получение информации о пользователе
  async getUserInfo(userId) {
    return API.get(`/users/${userId}`);
  }
};

export default userService;