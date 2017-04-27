/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* eslint-disable import/no-unresolved, import/default */

import deviceAliasesTemplate from './device-aliases.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AddWidgetController($scope, widgetService, deviceService, $mdDialog, $q, $document, types, dashboard, aliasesInfo, widget, widgetInfo) {

    var vm = this;

    vm.dashboard = dashboard;
    vm.aliasesInfo = aliasesInfo;
    vm.widget = widget;
    vm.widgetInfo = widgetInfo;

    vm.functionsOnly = false;

    vm.helpLinkIdForWidgetType = helpLinkIdForWidgetType;
    vm.add = add;
    vm.cancel = cancel;
    vm.fetchDeviceKeys = fetchDeviceKeys;
    vm.createDeviceAlias = createDeviceAlias;

    vm.widgetConfig = vm.widget.config;

    var settingsSchema = vm.widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
    var dataKeySettingsSchema = vm.widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;

    if (!settingsSchema || settingsSchema === '') {
        vm.settingsSchema = {};
    } else {
        vm.settingsSchema = angular.fromJson(settingsSchema);
    }
    if (!dataKeySettingsSchema || dataKeySettingsSchema === '') {
        vm.dataKeySettingsSchema = {};
    } else {
        vm.dataKeySettingsSchema = angular.fromJson(dataKeySettingsSchema);
    }

    function helpLinkIdForWidgetType() {
        var link = 'widgetsConfig';
        if (vm.widget && vm.widget.type) {
            switch (vm.widget.type) {
                case types.widgetType.timeseries.value: {
                    link = 'widgetsConfigTimeseries';
                    break;
                }
                case types.widgetType.latest.value: {
                    link = 'widgetsConfigLatest';
                    break;
                }
                case types.widgetType.rpc.value: {
                    link = 'widgetsConfigRpc';
                    break;
                }
                case types.widgetType.static.value: {
                    link = 'widgetsConfigStatic';
                    break;
                }
            }
        }
        return link;
    }

    function cancel () {
        $mdDialog.cancel({aliasesInfo: vm.aliasesInfo});
    }

    function add () {
        if ($scope.theForm.$valid) {
            $scope.theForm.$setPristine();
            vm.widget.config = vm.widgetConfig;
            $mdDialog.hide({widget: vm.widget, aliasesInfo: vm.aliasesInfo});
        }
    }

    function fetchDeviceKeys (deviceAliasId, query, type) {
        var deviceAlias = vm.aliasesInfo.deviceAliases[deviceAliasId];
        if (deviceAlias && deviceAlias.deviceId) {
            return deviceService.getDeviceKeys(deviceAlias.deviceId, query, type);
        } else {
            return $q.when([]);
        }
    }

    function createDeviceAlias (event, alias) {

        var deferred = $q.defer();
        var singleDeviceAlias = {id: null, alias: alias, deviceFilter: null};

        $mdDialog.show({
            controller: 'DeviceAliasesController',
            controllerAs: 'vm',
            templateUrl: deviceAliasesTemplate,
            locals: {
                config: {
                    deviceAliases: angular.copy(vm.dashboard.configuration.deviceAliases),
                    widgets: null,
                    isSingleDeviceAlias: true,
                    singleDeviceAlias: singleDeviceAlias
                }
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            skipHide: true,
            targetEvent: event
        }).then(function (singleDeviceAlias) {
            vm.dashboard.configuration.deviceAliases[singleDeviceAlias.id] =
                { alias: singleDeviceAlias.alias, deviceFilter: singleDeviceAlias.deviceFilter };
            deviceService.processDeviceAliases(vm.dashboard.configuration.deviceAliases).then(
                function(resolution) {
                    if (!resolution.error) {
                        vm.aliasesInfo = resolution.aliasesInfo;
                    }
                    deferred.resolve(singleDeviceAlias);
                }
            );
        }, function () {
            deferred.reject();
        });

        return deferred.promise;
    }
}
