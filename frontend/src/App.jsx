import { BrowserRouter, Routes, Route } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
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
import { ChatFloating } from './components/ChatWidget'
import ToastContainer from './components/ToastContainer'
import ErrorBoundary from './components/ErrorBoundary'

function App() {
    return (
        <ErrorBoundary>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />
                    <Route path="/search" element={<SearchPage />} />
                    <Route path="/movie/:id" element={<MovieDetailPage />} />
                    <Route path="/profile/:username" element={<ProfilePage />} />

                    {/* Rotte protette — richiedono login */}
                    <Route path="/library" element={<ProtectedRoute><LibraryPage /></ProtectedRoute>} />
                    <Route path="/chat" element={<ProtectedRoute><ChatAiPage /></ProtectedRoute>} />
                    <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />

                    {/* Rotte admin */}
                    <Route path="/admin" element={<ProtectedRoute adminOnly><AdminPage /></ProtectedRoute>} />
                </Routes>

                {/* Chat AI floating — visibile in tutte le pagine */}
                <ChatFloating />
                <ToastContainer />
            </BrowserRouter>
        </ErrorBoundary>
    )
}

export default App