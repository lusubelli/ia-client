import { Routes, RouterModule } from '@angular/router';

import { HomeComponent } from './home/home.component';
import { FaceComponent } from './face/face.component';
import { IdentitiesComponent } from './identities/identities.component';

const appRoutes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'inbox', component: FaceComponent },
  { path: 'identities', component: IdentitiesComponent },

  // otherwise redirect to home
  { path: '**', redirectTo: '' }
];

export const routing = RouterModule.forRoot(appRoutes);
