import {Component} from '@angular/core';

@Component({
  selector: 'modal',
  template: '<div class="overlay" *ngIf="open"><ng-content></ng-content></div>',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {

  constructor() {}

  open = false

  show() {
    this.open = true;
  }

  hide() {
    this.open = false;
  }

}
