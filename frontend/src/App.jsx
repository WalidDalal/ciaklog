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
import WrappedPage from './pages/WrappedPage'
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
                    {/* Fix: prima /profile/:username era pubblica ma faceva una
                        Promise.all con /reviews/user/{username} che richiede login —
                        da sloggato la seconda chiamata falliva (401), Promise.all
                        falliva tutto insieme, e il catch silenzioso mostrava
                        "Utente non trovato" anche se l'utente esisteva davvero.
                        Ora la rotta è protetta: redirect pulito al login, che
                        torna qui dopo l'accesso (ProtectedRoute + LoginPage
                        gestiscono già state.from) */}
                    <Route path="/profile/:username" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />

                    {/* Rotte protette — richiedono login */}
                    <Route path="/library" element={<ProtectedRoute><LibraryPage /></ProtectedRoute>} />
                    <Route path="/chat" element={<ProtectedRoute><ChatAiPage /></ProtectedRoute>} />
                    <Route path="/settings" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
                    {/* Fix (CiakLog Wrapped): protetta come le altre pagine personali —
                        se un Admin ci arriva, il backend risponde 403 (stesso pattern
                        di libreria/recensioni/chat, non ha dati da vedere comunque) */}
                    <Route path="/wrapped" element={<ProtectedRoute><WrappedPage /></ProtectedRoute>} />

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