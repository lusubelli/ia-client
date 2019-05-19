import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {routing} from "./app.routing";

import {TabsComponent} from './tabs/tabs.component';
import {TabComponent} from './tab/tab.component';
import {AppComponent} from './app.component';
import {FaceComponent} from "./face/face.component";
import {HomeComponent} from './home/home.component';
import {IdentitiesComponent} from './identities/identities.component';

@NgModule({
  declarations: [
    AppComponent,
    TabsComponent,
    TabComponent,
    FaceComponent,
    IdentitiesComponent,
    HomeComponent
  ],
  imports: [
    BrowserModule,
    routing
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {
}
