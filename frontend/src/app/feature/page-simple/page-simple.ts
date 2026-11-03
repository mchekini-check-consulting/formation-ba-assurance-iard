import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Page vide, avec seulement son titre (donné par la route, voir app.routes.ts) : contenu à venir. */
@Component({
  selector: 'iard-page-simple',
  template: '<main><h1>{{ titre }}</h1></main>',
})
export class PageSimple {
  protected readonly titre: string = inject(ActivatedRoute).snapshot.data['titre'];
}
