import { NgModule, APP_INITIALIZER } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { AppConfigService } from './providers/app-config.service'
import { AppRoutingModule } from './app-routing.module';
import { HomeComponent } from './home/home.component';
import { ModuleListComponent } from './modulelist/modulelist.component';
import { ModuleComponent } from './module/module.component';
import { ModuleService } from './services/module.service';
import { HttpErrorHandler } from './services/http-error-handler.service';
import { MessageService } from './services/message.service';

export function initConfig(appConfig: AppConfigService) {
  return () => appConfig.loadConfig();
}



@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    ModuleListComponent,
    ModuleComponent
  ],
  imports: [
    BrowserModule.withServerTransition({ appId: 'serverApp' }),
    FormsModule, ReactiveFormsModule,
    HttpClientModule,
    AppRoutingModule
  ],
  providers: [
    {
      provide: APP_INITIALIZER, useFactory: initConfig,  deps: [AppConfigService],  multi: true
    },
    ModuleService, HttpErrorHandler, MessageService
  ],
  bootstrap: [AppComponent]
})

export class AppModule {}
