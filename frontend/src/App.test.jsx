import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

beforeEach(() =>
  vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {}))),
)

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  window.location.hash = ''
})

describe('App', () => {
  it("affiche la landing habitation en page d'accueil", () => {
    render(<App />)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe(
      'Votre logement protégé, sans mauvaise surprise.',
    )
  })

  it("affiche l'écran Items sur #/items", () => {
    window.location.hash = '#/items'
    render(<App />)
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Item')
  })

  it("change d'écran quand l'adresse change", () => {
    render(<App />)
    act(() => {
      window.location.hash = '#/items'
      window.dispatchEvent(new HashChangeEvent('hashchange'))
    })
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Item')
  })
})
