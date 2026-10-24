import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Contrats from './Contrats'

// Réponse fetch simulée (JSON, pour /contrats)
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

describe('Contrats', () => {
  it('affiche les contrats du backend contrats', async () => {
    fetch.mockReturnValueOnce(
      reponse([{ id: 1, numero: 'CTR-001', assure: 'Jean Dupont' }]),
    )
    render(<Contrats />)
    expect(await screen.findByText('CTR-001')).toBeTruthy()
    expect(screen.getByText(': Jean Dupont', { exact: false })).toBeTruthy()
    expect(fetch).toHaveBeenCalledWith('/api/contrats/contrats', undefined)
  })

  it('ajoute un contrat', async () => {
    fetch
      .mockReturnValueOnce(reponse([]))
      .mockReturnValueOnce(
        reponse({ id: 2, numero: 'CTR-002', assure: 'Marie Martin' }, 201),
      )
    render(<Contrats />)
    await screen.findByText('Aucun contrat')
    fireEvent.change(screen.getByRole('textbox', { name: 'Numéro' }), {
      target: { value: 'CTR-002' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: 'Assuré' }), {
      target: { value: 'Marie Martin' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter le contrat' }))
    expect(await screen.findByText('CTR-002')).toBeTruthy()
    const [url, options] = fetch.mock.calls[1]
    expect(url).toBe('/api/contrats/contrats')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body)).toEqual({
      numero: 'CTR-002',
      assure: 'Marie Martin',
    })
    expect(screen.getByRole('textbox', { name: 'Numéro' }).value).toBe('')
    expect(screen.getByRole('textbox', { name: 'Assuré' }).value).toBe('')
  })

  it('signale une erreur du backend contrats', async () => {
    fetch.mockReturnValueOnce(reponse('', 503)).mockReturnValueOnce(reponse('', 400))
    render(<Contrats />)
    expect((await screen.findByRole('alert')).textContent).toContain('503')
    fireEvent.change(screen.getByRole('textbox', { name: 'Numéro' }), {
      target: { value: 'x' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: 'Assuré' }), {
      target: { value: 'y' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter le contrat' }))
    expect(
      (await screen.findByText(/Ajout du contrat impossible/)).textContent,
    ).toContain('400')
  })
})
