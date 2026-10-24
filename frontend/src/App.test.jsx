import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { apiBase } from './api'

// Réponse fetch simulée
function reponse(corps, status = 200) {
  return Promise.resolve({
    ok: status < 400,
    status,
    json: () => Promise.resolve(corps),
  })
}

beforeEach(() => vi.stubGlobal('fetch', vi.fn()))

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('apiBase', () => {
  it("pointe la passerelle de l'environnement du frontend", () => {
    expect(apiBase('frontend.formation-ba-projet.qualif.check-consulting.net')).toBe(
      'https://api.qualif.check-consulting.net/formation-ba-projet/backend',
    )
  })

  it('utilise le proxy du serveur de dev en local', () => {
    expect(apiBase('localhost')).toBe('/api')
  })
})

describe('App', () => {
  it('affiche les items du backend', async () => {
    fetch.mockReturnValueOnce(reponse([{ id: 1, nom: 'a', description: 'd' }]))
    render(<App />)
    expect(await screen.findByText('a')).toBeTruthy()
    expect(screen.getByText(': d')).toBeTruthy()
    expect(fetch).toHaveBeenCalledWith('/api/items', undefined)
  })

  it('affiche le total au singulier avec un seul item', async () => {
    fetch.mockReturnValueOnce(reponse([{ id: 1, nom: 'a', description: '' }]))
    render(<App />)
    expect(await screen.findByText('Total : 1 item')).toBeTruthy()
  })

  it('affiche le total au pluriel avec plusieurs items', async () => {
    fetch.mockReturnValueOnce(
      reponse([
        { id: 1, nom: 'a', description: '' },
        { id: 2, nom: 'b', description: '' },
      ]),
    )
    render(<App />)
    expect(await screen.findByText('Total : 2 items')).toBeTruthy()
  })

  it('ajoute un item', async () => {
    fetch
      .mockReturnValueOnce(reponse([]))
      .mockReturnValueOnce(reponse({ id: 2, nom: 'Premier', description: null }, 201))
    render(<App />)
    await screen.findByText('Aucun item')
    fireEvent.change(screen.getByRole('textbox', { name: 'Nom' }), {
      target: { value: 'Premier' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter' }))
    expect(await screen.findByText('Premier')).toBeTruthy()
    const [url, options] = fetch.mock.calls[1]
    expect(url).toBe('/api/items')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body)).toEqual({ nom: 'Premier', description: '' })
    expect(screen.getByRole('textbox', { name: 'Nom' }).value).toBe('')
  })

  it('signale une erreur du backend', async () => {
    fetch.mockReturnValueOnce(reponse('', 503)).mockReturnValueOnce(reponse('', 400))
    render(<App />)
    expect((await screen.findByRole('alert')).textContent).toContain('503')
    fireEvent.change(screen.getByRole('textbox', { name: 'Nom' }), {
      target: { value: 'x' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter' }))
    expect((await screen.findByText(/Ajout impossible/)).textContent).toContain('400')
  })
})
