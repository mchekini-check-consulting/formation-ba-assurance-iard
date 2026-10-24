import { useEffect, useState } from 'react'
import { creerItem, listerItems } from './api'
import './App.css'

// Liste des items du backend (GET) et formulaire d'ajout (POST)
// Petit commentaire ajouté pour feat/testfonc
function App() {
  const [items, setItems] = useState([])
  const [nom, setNom] = useState('')
  const [description, setDescription] = useState('')
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    listerItems()
      .then(setItems)
      .catch((e) => setErreur(`Chargement impossible (${e.message})`))
  }, [])

  async function ajouter(event) {
    event.preventDefault()
    setErreur('')
    try {
      const item = await creerItem({ nom, description })
      setItems((liste) => [...liste, item])
      setNom('')
      setDescription('')
    } catch (e) {
      setErreur(`Ajout impossible (${e.message})`)
    }
  }

  return (
    <main className="items">
      <h1>Items</h1>

      <form onSubmit={ajouter}>
        <label>
          Nom
          <input
            name="nom"
            value={nom}
            onChange={(e) => setNom(e.target.value)}
            required
            maxLength={200}
          />
        </label>
        <label>
          Description
          <input
            name="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={1000}
          />
        </label>
        <button type="submit">Ajouter</button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      <p className="compteur">Total : {items.length} item{items.length > 1 ? 's' : ''}</p>

      <ul>
        {items.length === 0 && <li className="vide">Aucun item</li>}
        {items.map((item) => (
          <li key={item.id}>
            <strong>{item.nom}</strong>
            {item.description && <span> : {item.description}</span>}
          </li>
        ))}
      </ul>
    </main>
  )
}

export default App
