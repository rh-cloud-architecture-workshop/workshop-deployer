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
  localModulelist = new ModuleList();
  moduleService: ModuleService;
  subscription: Subscription;
  router: Router
  interval:any;
  showFilterCard:boolean = true;
  globalConfig;

  //warning messages
  alertMessages = {
    "MAX_MODULES_DEPLOYED" : {"type":"warning", "text": "You have reached the maximum number of deployable modules. Please undeploy any of the already deployed modules to deploy additional modules."}
  }
  currentAlert = null;
  filterDeployedModules:boolean = false;
  uniqueModuleTags;

  constructor(moduleService: ModuleService, router: Router, @Inject(PLATFORM_ID) platformId:string) {
    this.testBrowser = isPlatformBrowser(platformId);
    this.moduleService = moduleService;
    this.router = router;
  }

  ngOnInit(): void {
    if (this.testBrowser) {
      this.fetchModuleList();
      this.getGlobalConfig();
      this.interval = setInterval(()=>{
        this.fetchModuleList();
      },10000);
    }
  }

  fetchModuleList() {
    this.moduleService.fetchModuleList().subscribe(modulelist => {
      this.modulelist = modulelist;
      this.getModuleTagFilters();
    });
  }

  getGlobalConfig() {
    this.moduleService.getGlobalConfig().subscribe(globalConfig => {
      console.log("globalConfig", globalConfig)
      this.globalConfig  = globalConfig;

    });
  }

  receiveMessage($event) {
    this.refreshComponent();
  }


  showAlert(errorCode) {
    this.currentAlert = this.alertMessages.MAX_MODULES_DEPLOYED;
  }

  refreshComponent() {
    this.fetchModuleList();
  }

  getDeployedModules() {
    if(this.modulelist.modules){
      return this.modulelist.modules.filter(o => o.deployed ===  true);
    } else{
      return []
    }
  }

  getFilteredModules() {
    //if "display only deployed modules" is ON, then reset all the filter flags
    if(this.filterDeployedModules){
      this.resetFilterTags();
      return  this.modulelist.modules.filter(o => o.deployed ===  true);
    }

    //if there are filter tags, then filter based on selections
    if(this.filterTagsCount() > 0) {
      return this.modulelist.modules.filter((module) => {
        return this.uniqueModuleTags.some((tag) => {
          return  module.primaryTags.indexOf(tag.name) > -1 && tag.status === true;
        });
      });
    }
    //return modules list - this maybe filtered if there are any selected filter tags
    return  this.modulelist.modules;

  }

  resetFilterTags() {
    this.uniqueModuleTags.forEach((tag) => {tag.status = false;})
  }

  filterTagsCount() {
    return (this.uniqueModuleTags) ? (this.uniqueModuleTags.filter(n => n.status).length) : 0;
  }

  //this runs just once  - when the page loads we get the list of all tags and dont need to do this repeatedly for every module status refresh
  getModuleTagFilters() {
    if(this.uniqueModuleTags == null) {
      var uniqueTags = [...new Set(this.modulelist.modules.map(item => item.primaryTags))].flat()
                      .filter((value, index, array) => array.indexOf(value) === index);
      this.uniqueModuleTags = uniqueTags.reduce(function(acc, cur, i) {
        acc[i] = {"name":cur, "status":false};
        return acc;
      }, []);
    }
  }

  toggleTag(tag) {
    tag.status = !tag.status;
    if(tag.status) {
      this.filterDeployedModules = false;
    }
  }

}
