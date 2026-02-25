import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import SimulationCall from './pages/SimulationCall';
import SimulationResult from './pages/SimulationResult';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/simulation/:scenarioId" element={<SimulationCall />} />
        <Route path="/result/:sessionId" element={<SimulationResult />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
