/*
 * Copyright (c) 2013-2018, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2018, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

package nextflow.k8s

import nextflow.cli.CliOptions
import nextflow.cli.CmdRun
import nextflow.cli.Launcher
import nextflow.k8s.client.ClientConfig
import nextflow.k8s.client.K8sClient
import spock.lang.Specification
import spock.lang.Unroll
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class K8sDriverLauncherTest extends Specification {

    @Unroll
    def 'should get cmd cli' () {

        given:
        def l = new K8sDriverLauncher(cmd: cmd, pipelineName: 'foo')

        when:
        cmd.launcher = new Launcher(options: new CliOptions())
        then:
        l.getLaunchCli() == expected

        where:
        cmd                                         | expected
        new CmdRun()                                | 'nextflow run foo'
        new CmdRun(cacheable: false)                | 'nextflow run foo -cache false'
        new CmdRun(resume: true)                    | 'nextflow run foo -resume true'
        new CmdRun(poolSize: 10)                    | 'nextflow run foo -ps 10'
        new CmdRun(pollInterval: 5)                 | 'nextflow run foo -pi 5'
        new CmdRun(queueSize: 9)                    | 'nextflow run foo -qs 9'
        new CmdRun(revision: 'xyz')                 | 'nextflow run foo -r xyz'
        new CmdRun(latest: true)                    | 'nextflow run foo -latest true'
        new CmdRun(withTrace: true)                 | 'nextflow run foo -with-trace true'
        new CmdRun(withTimeline: true)              | 'nextflow run foo -with-timeline true'
        new CmdRun(withDag: true)                   | 'nextflow run foo -with-dag true'
        new CmdRun(profile: 'ciao')                 | 'nextflow run foo -profile ciao'
        new CmdRun(dumpHashes: true)                | 'nextflow run foo -dump-hashes true'
        new CmdRun(dumpChannels: 'lala')            | 'nextflow run foo -dump-channels lala'
        new CmdRun(env: [XX:'hello', YY: 'world'])  | 'nextflow run foo -e.XX hello -e.YY world'
        new CmdRun(process: [mem: '100',cpus:'2'])  | 'nextflow run foo -process.mem 100 -process.cpus 2'
        new CmdRun(params: [alpha:'x', beta:'y'])   | 'nextflow run foo --alpha x --beta y'
        new CmdRun(params: [alpha: '/path/*.txt'])  | 'nextflow run foo --alpha /path/\\*.txt'
    }

    def 'should set the run name' () {
        given:
        def cmd = new CmdRun()
        cmd.launcher = new Launcher(options: new CliOptions())

        when:
        def l = new K8sDriverLauncher(cmd: cmd, pipelineName: 'foo', runName: 'bar')
        then:
        l.getLaunchCli() == 'nextflow run foo -name bar'
    }

    @Unroll
    def 'should get pod name' () {

        given:
        def l = new K8sDriverLauncher(runName: name)

        expect:
        l.getPodName() == expect
        where:
        name        | expect
        'foo'       | 'nf-run-foo'
        'foo_bar'   | 'nf-run-foo-bar'
    }


    def 'should create config' () {

        given:
        K8sClient client
        def driver = Spy(K8sDriverLauncher)
        def CLIENT_CFG = [server: 'foo.com', token: '']
        
        when:
        client = driver.createK8sClient([:])
        then:
        1 * driver.configDiscover() >> new ClientConfig(server: 'http://k8s.com:8000', token: 'xyz')
        client.config.server == 'http://k8s.com:8000'
        client.config.token == 'xyz'
        client.config.namespace == 'default'
        client.config.serviceAccount == null

        when:
        def cfg = [k8s: [client:CLIENT_CFG, namespace: 'my-namespace', serviceAccount: 'my-account']]
        client = driver.createK8sClient(cfg)
        then:
        1 * driver.configCreate(CLIENT_CFG) >> { ClientConfig.fromMap(CLIENT_CFG) }
        client.config.server == 'foo.com'
        client.config.namespace == 'my-namespace'
        client.config.serviceAccount == 'my-account'

    }

    def 'should create launcher spec' () {

        given:
        def driver = Spy(K8sDriverLauncher)

        when:
        driver.userDir = '/the/user/dir'
        driver.workDir = '/the/work/dir'
        driver.projectDir = '/the/project/dir'
        driver.runName = 'the-run-name'
        driver.configMounts['cfg1'] = '/mnt/vol1'
        driver.client = new K8sClient(new ClientConfig(namespace: 'foo', serviceAccount: 'bar'))

        def spec = driver.makeLauncherSpec()
        then:
        driver.getPodName() >> 'nf-pod'
        driver.getImageName() >> 'the-image'
        driver.getVolumeClaims() >> new VolumeClaims( vol1: [mountPath: '/mnt/vol1'] )
        driver.getLaunchCli() >> 'nextflow run foo'

        spec == [apiVersion: 'v1',
                 kind: 'Pod',
                 metadata: [name:'nf-pod', namespace:'foo', labels:[app:'nextflow', runName:'the-run-name']],
                 spec: [restartPolicy:'Never',
                        containers:[
                                [name:'nf-pod',
                                 image:'the-image',
                                 command:['/bin/bash', '-c', "mkdir -p '/the/user/dir'; if [ -d '/the/user/dir' ]; then cd '/the/user/dir'; else echo 'Cannot create nextflow userDir: /the/user/dir'; exit 1; fi; [ -f /etc/nextflow/scm ] && ln -s /etc/nextflow/scm \$NXF_HOME/scm; [ -f /etc/nextflow/nextflow.config ] && cp /etc/nextflow/nextflow.config nextflow.config; nextflow run foo"],
                                 env:[
                                         [name:'NXF_WORK', value:'/the/work/dir'],
                                         [name:'NXF_ASSETS', value:'/the/project/dir'],
                                         [name:'NXF_EXECUTOR', value:'k8s']],
                                 volumeMounts:[
                                         [name:'vol-1', mountPath:'/mnt/vol1'],
                                         [name:'vol-2', mountPath:'/mnt/vol1']]]
                                ],
                        serviceAccountName:'bar',
                        volumes:[[name:'vol-1', persistentVolumeClaim:[claimName:'vol1']],
                                 [name:'vol-2', configMap:[name:'cfg1'] ]]
                 ]
        ]


    }
}
