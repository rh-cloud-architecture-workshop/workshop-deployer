import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { ModuleList, Module } from '../models/module.model';
import { ModuleService } from '../services/module.service';

@Component({
  selector: 'app-modulelist',
  templateUrl: './modulelist.component.html',
  styleUrls: ['./modulelist.component.css']
})

export class ModuleListComponent implements OnInit {

  testBrowser: boolean;
  modulelist = new ModuleList();
  moduleService: ModuleService;
  subscription: Subscription;
  router: Router
  interval:any;

  constructor(moduleService: ModuleService, router: Router, @Inject(PLATFORM_ID) platformId:string) {
    this.testBrowser = isPlatformBrowser(platformId);
    this.moduleService = moduleService;
    this.router = router;
  }

  ngOnInit(): void {
    if (this.testBrowser) {
      this.fetchModuleList();
      this.interval = setInterval(()=>{
        this.fetchModuleList();
      },10000);
    }
  }

  fetchModuleList() {
    this.moduleService.fetchModuleList().subscribe(modulelist => (this.modulelist = modulelist));
  }

  receiveMessage($event) {
    this.refreshComponent();
  }

  refreshComponent() {
    this.fetchModuleList();
  }

}
