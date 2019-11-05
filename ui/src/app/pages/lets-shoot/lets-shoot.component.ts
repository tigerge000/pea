import { Component, OnInit } from '@angular/core'
import { Router } from '@angular/router'
import { TranslateService } from '@ngx-translate/core'
import { NzModalService } from 'ng-zorro-antd'
import { Observable } from 'rxjs'
import { HomeService, WorkerData } from 'src/app/api/home.service'
import { ApiRes } from 'src/app/model/api.model'
import {
  HttpMethods,
  RunSimulationJob,
  SimulationModel,
  SingleHttpScenarioJob,
  SingleHttpScenarioMessage,
  WorkersAvailable,
} from 'src/app/model/pea.model'

@Component({
  selector: 'app-lets-shoot',
  templateUrl: './lets-shoot.component.html',
  styleUrls: ['./lets-shoot.component.css']
})
export class LetsShootComponent implements OnInit {

  METHODS = [HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT]
  tabIndex = 0
  loading = false

  innerRequest: SingleHttpScenarioMessage = {
    request: {
      method: HttpMethods.GET,
    },
    injections: []
  }

  simulation: SimulationModel = {}
  selectedWorkers: WorkerData[] = []

  headersStr = ''

  lastCompileTime = ''
  editorBaseUrl = ''
  simulations: SimulationModel[] = []

  constructor(
    private homeService: HomeService,
    private modalService: NzModalService,
    private i18nService: TranslateService,
    private router: Router,
  ) { }

  edit() {
    if (this.editorBaseUrl && this.simulation.name) {
      const url = `${this.editorBaseUrl}${this.simulation.name.replace(/\./g, '/')}.scala`
      window.open(url)
    }
  }

  run() {
    if (this.loading) return
    let response: Observable<ApiRes<WorkersAvailable>>
    if (this.tabIndex === 0) {
      this.loading = true
      response = this.homeService.runSingleHttpScenarioJob(this.buildLoadJob())
    } else if (this.tabIndex === 1) {
      this.loading = true
      response = this.homeService.runSimulationJob(this.buildLoadJob())
    }
    response.subscribe(res => {
      this.loading = false
      if (res.data.available) {
        this.modalService.create({
          nzTitle: res.data.runId,
          nzContent: this.i18nService.instant('tips.jobSuccess'),
          nzClosable: true,
          nzOnOk: () => {
            this.router.navigateByUrl(`/jobs/${res.data.runId}`)
          }
        })
      } else {
        this.modalService.create({
          nzTitle: this.i18nService.instant('tips.jobFail'),
          nzContent: JSON.stringify(res.data.errors),
          nzClosable: true
        })
      }
    }, _ => this.loading = false)
  }

  buildLoadJob(): SingleHttpScenarioJob | RunSimulationJob {
    if (this.tabIndex === 0) {
      if (this.headersStr) {
        try {
          this.innerRequest.request.headers = JSON.parse(this.headersStr)
        } catch (error) {
          console.error(error)
        }
      }
      return {
        workers: this.selectedWorkers.map(item => item.member),
        request: this.innerRequest
      }
    } else if (this.tabIndex === 1) {
      return {
        workers: this.selectedWorkers.map(item => item.member),
        request: {
          simulation: this.simulation.name
        }
      }
    }
  }

  loadSimulations() {
    this.homeService.getSimulations().subscribe(res => {
      this.lastCompileTime = new Date(res.data.last).toLocaleString()
      this.editorBaseUrl = res.data.editorBaseUrl
      this.simulations = res.data.simulations
      if (!this.simulation.name && this.simulations.length > 0) {
        this.simulation = this.simulations[0]
      }
    })
  }

  ngOnInit(): void {
    this.loadSimulations()
  }
}
