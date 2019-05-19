import { Component, Input } from '@angular/core';

@Component({
  selector: 'my-tab',
  templateUrl: './tab.component.html',
  styleUrls: ['./tab.component.css']
})
export class TabComponent {
  @Input('tabTitle') title: string;
  @Input() active = false;
}
