import { useEffect, useState } from 'react'
import { creerContrat, listerContrats } from './api'
import './App.css'

// Liste des contrats du backend contrats (GET) et formulaire d'ajout (POST)
function Contrats() {
  const [contrats, setContrats] = useState([])
  const [numero, setNumero] = useState('')
  const [assure, setAssure] = useState('')
  const [erreur, setErreur] = useState('')

  useEffect(() => {
    listerContrats()
      .then(setContrats)
      .catch((e) => setErreur(`Chargement des contrats impossible (${e.message})`))
  }, [])

  async function ajouter(event) {
    event.preventDefault()
    setErreur('')
    try {
      const contrat = await creerContrat({ numero, assure })
      setContrats((liste) => [...liste, contrat])
      setNumero('')
      setAssure('')
    } catch (e) {
      setErreur(`Ajout du contrat impossible (${e.message})`)
    }
  }

  return (
    <section className="items">
      <h1>Contrats</h1>

      <form onSubmit={ajouter}>
        <label>
          Numéro{' '}
          <input
            name="numero"
            value={numero}
            onChange={(e) => setNumero(e.target.value)}
            required
            maxLength={50}
          />
        </label>
        <label>
          Assuré{' '}
          <input
            name="assure"
            value={assure}
            onChange={(e) => setAssure(e.target.value)}
            required
            maxLength={200}
          />
        </label>
        <button type="submit">Ajouter le contrat</button>
      </form>

      {erreur && (
        <p className="erreur" role="alert">
          {erreur}
        </p>
      )}

      <ul>
        {contrats.length === 0 && <li className="vide">Aucun contrat</li>}
        {contrats.map((contrat) => (
          <li key={contrat.id}>
            <strong>{contrat.numero}</strong> : {contrat.assure}
          </li>
        ))}
      </ul>
    </section>
  )
}

export default Contrats
