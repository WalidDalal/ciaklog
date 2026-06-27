import { BrowserRouter, Routes, Route } from 'react-router-dom'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import LibraryPage from './pages/LibraryPage'
import SearchPage from './pages/SearchPage'
import MovieDetailPage from './pages/MovieDetailPage'
import ProfilePage from './pages/ProfilePage'
import ChatAiPage from './pages/ChatAiPage'
import AdminPage from './pages/AdminPage'
import SettingsPage from './pages/SettingsPage'
import { ChatAiFloatingButton } from './components/Navbar'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/library" element={<LibraryPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/movie/:id" element={<MovieDetailPage />} />
        <Route path="/profile/:username" element={<ProfilePage />} />
        <Route path="/chat" element={<ChatAiPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Routes>
      {/* Floating button AI — visibile in tutte le pagine tranne /chat */}
      <ChatAiFloatingButton />
    </BrowserRouter>
  )
}

export default App