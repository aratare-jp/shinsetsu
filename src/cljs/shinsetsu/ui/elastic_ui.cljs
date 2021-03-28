(ns shinsetsu.ui.elastic-ui
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    ["@elastic/eui" :refer [EuiButton
                            EuiCheckboxGroup
                            EuiFieldText
                            EuiForm
                            EuiFormRow
                            EuiFilePicker
                            EuiLink
                            EuiRange
                            EuiSelect
                            EuiSpacer
                            EuiSwitch
                            EuiText
                            EuiPage
                            EuiPageBody
                            EuiPageContent
                            EuiPageContentBody
                            EuiPageContentHeader
                            EuiPageContentHeaderSection
                            EuiTitle
                            EuiControlBar
                            EuiIcon
                            EuiTabs
                            EuiTab
                            EuiTabbedContent
                            EuiText
                            EuiCard
                            EuiFlexGroup
                            EuiFlexGrid
                            EuiFlexItem
                            EuiModal
                            EuiModalHeader
                            EuiModalBody
                            EuiModalFooter
                            EuiButtonEmpty
                            EuiModalHeaderTitle
                            EuiOverlayMask]]))

(def ui-button (interop/react-factory EuiButton))
(def ui-checkbox-group (interop/react-factory EuiCheckboxGroup))
(def ui-field-text (interop/react-factory EuiFieldText))
(def ui-form (interop/react-factory EuiForm))
(def ui-form-row (interop/react-factory EuiFormRow))
(def ui-file-picker (interop/react-factory EuiFilePicker))
(def ui-link (interop/react-factory EuiLink))
(def ui-range (interop/react-factory EuiRange))
(def ui-select (interop/react-factory EuiSelect))
(def ui-spacer (interop/react-factory EuiSpacer))
(def ui-switch (interop/react-factory EuiSwitch))
(def ui-page (interop/react-factory EuiPage))
(def ui-page-body (interop/react-factory EuiPageBody))
(def ui-page-content (interop/react-factory EuiPageContent))
(def ui-page-content-body (interop/react-factory EuiPageContentBody))
(def ui-page-content-header (interop/react-factory EuiPageContentHeader))
(def ui-page-content-header-section (interop/react-factory EuiPageContentHeaderSection))
(def ui-title (interop/react-factory EuiTitle))
(def ui-control-bar (interop/react-factory EuiControlBar))
(def ui-icon (interop/react-factory EuiIcon))
(def ui-tabs (interop/react-factory EuiTabs))
(def ui-tab (interop/react-factory EuiTab))
(def ui-text (interop/react-factory EuiText))
(def ui-tabbed-content (interop/react-factory EuiTabbedContent))
(def ui-card (interop/react-factory EuiCard))
(def ui-flex-group (interop/react-factory EuiFlexGroup))
(def ui-flex-grid (interop/react-factory EuiFlexGrid))
(def ui-flex-item (interop/react-factory EuiFlexItem))
(def ui-modal (interop/react-factory EuiModal))
(def ui-modal-header (interop/react-factory EuiModalHeader))
(def ui-modal-header-title (interop/react-factory EuiModalHeaderTitle))
(def ui-modal-body (interop/react-factory EuiModalBody))
(def ui-modal-footer (interop/react-factory EuiModalFooter))
(def ui-button-empty (interop/react-factory EuiButtonEmpty))
(def ui-overlay-mask (interop/react-factory EuiOverlayMask))

