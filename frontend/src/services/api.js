import axios from 'axios';

export const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

// Axios client with authorization interceptor
const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('boardroom_jwt');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Auth Services
export const registerUser = async (username, email, password) => {
  const res = await api.post('/auth/register', { username, email, password });
  if (res.data?.token) {
    localStorage.setItem('boardroom_jwt', res.data.token);
    localStorage.setItem('boardroom_user', JSON.stringify(res.data.user));
  }
  return res.data;
};

export const loginUser = async (usernameOrEmail, password) => {
  const res = await api.post('/auth/login', { usernameOrEmail, password });
  if (res.data?.token) {
    localStorage.setItem('boardroom_jwt', res.data.token);
    localStorage.setItem('boardroom_user', JSON.stringify(res.data.user));
  }
  return res.data;
};

export const getMe = async () => {
  const res = await api.get('/auth/me');
  return res.data;
};

export const logoutUser = () => {
  localStorage.removeItem('boardroom_jwt');
  localStorage.removeItem('boardroom_user');
};

export const getStoredUser = () => {
  try {
    const raw = localStorage.getItem('boardroom_user');
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
};

// Room Services
export const createRoom = async (topic, password, maxMembers) => {
  const res = await api.post('/rooms/create', { topic, password, maxMembers: Number(maxMembers) });
  return res.data;
};

export const joinRoom = async (roomCode, password) => {
  const res = await api.post('/rooms/join', { roomCode, password });
  return res.data;
};

export const getRoom = async (roomId) => {
  const res = await api.get(`/rooms/${roomId}`);
  return res.data;
};

export const getRoomMembers = async (roomId) => {
  const res = await api.get(`/rooms/${roomId}/members`);
  return res.data;
};

export const startRoomSession = async (roomId) => {
  const res = await api.post(`/rooms/${roomId}/start`);
  return res.data;
};

export const endRoomSession = async (roomId) => {
  const res = await api.post(`/rooms/${roomId}/end`);
  return res.data;
};

export const leaveRoom = async (roomId) => {
  const res = await api.post(`/rooms/${roomId}/leave`);
  return res.data;
};

export const getRoomSwot = async (roomId) => {
  const res = await api.get(`/rooms/${roomId}/swot`);
  return res.data;
};

export const getRoomMessages = async (roomId) => {
  const res = await api.get(`/rooms/${roomId}/messages`);
  return res.data;
};

export const getTurnState = async (roomId) => {
  const res = await api.get(`/rooms/${roomId}/turn`);
  return res.data;
};

export default api;