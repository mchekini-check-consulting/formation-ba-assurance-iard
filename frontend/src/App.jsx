import { useEffect, useState } from 'react'
import Items from './Items'
import LandingHabitation from './LandingHabitation'

const routeCourante = () => window.location.hash

// Page d'accueil : la landing habitation ; l'écran Items reste sur /#/items
function App() {
  const [route, setRoute] = useState(routeCourante)

  useEffect(() => {
    const changer = () => setRoute(routeCourante())
    window.addEventListener('hashchange', changer)
    return () => window.removeEventListener('hashchange', changer)
  }, [])

  return route === '#/items' ? <Items /> : <LandingHabitation />
}

export default App
