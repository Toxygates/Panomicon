import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-layout-picker',
  templateUrl: './layout-picker.component.html',
  styleUrls: ['./layout-picker.component.scss']
})
export class LayoutPickerComponent implements OnInit {
	
	@Input() selectedLayout!: any;

	@Output() changeLayoutEmitter = new EventEmitter<string>();

	public layouts: any = [
		{ value:'null'         , id:'None' },
		{ value:'custom'       , id:'Custom'},
		{ value:'random'       , id:'Random'},
		{ value:'grid'         , id:'Grid'},
		{ value:'circle'       , id:'Circle'}, 
		{ value:'concentric'   , id:'Concentric'},
		{ value:'breadthfirst' , id:'Breadth First'},
		{ value:'cose'         , id:'Force Directed'}
	];

  constructor() { }

  ngOnInit(): void {
  }

	changeLayout(value:any):void{
		this.changeLayoutEmitter.emit(value);
	}

}
