import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Subscription } from 'rxjs';

import { Module } from '../models/module.model';
import { ModuleService } from '../services/module.service';

@Component({
  selector: 'app-module',
  templateUrl: './module.component.html',
  styleUrls: ['./module.component.css']
})
export class ModuleComponent {

  moduleService: ModuleService;

  @Input() module:Module;
  @Input() maxModulesReached:boolean = false;
  @Input() globalConfig:Map<String, String>;

  @Output() messageEvent = new EventEmitter<string>();
  @Output() alertEvent = new EventEmitter<string>();

  constructor(moduleService: ModuleService) {
    this.moduleService = moduleService;
  }

  deployApplication(application: string) {
     this.moduleService.deployApplication(application)
       .subscribe(response => {
          console.log(response);
          if (response.status == 'ok') {
            this.module.deployed = response.application.deployed;
            this.module.deleting = response.application.deleting;
            this.module.health = response.application.health;
            this.module.status = response.application.status;
          }
       });
  }

  undeployApplication(application: string) {
    if(confirm("Click OK to confirm, and Cancel to exit. This is action irreversible.")) {
      this.moduleService.undeployApplication(application)
       .subscribe(response => {
          console.log(response);
          if (response.status == 'ok') {
            this.module.deployed = response.application.deployed;
            this.module.deleting = response.application.deleting;
            this.module.health = response.application.health;
            this.module.status = response.application.status;
          }
       });
    }
  }

  sendMessage(message: string) {
    this.messageEvent.emit(message)
  }

  sendAlert(alert: string) {
    this.alertEvent.emit(alert)
  }

}
