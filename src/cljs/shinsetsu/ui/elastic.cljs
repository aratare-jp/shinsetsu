(ns shinsetsu.ui.elastic
  (:require
    [com.fulcrologic.fulcro.algorithms.react-interop :as ri]
    ["@emotion/cache$default" :as createCache]
    ["@elastic/eui" :refer [EuiFieldSearch
                            EuiHorizontalRule
                            EuiCollapsibleNav
                            EuiCollapsibleNavGroup
                            EuiHeader
                            EuiHeaderSection
                            EuiHeaderSectionItem
                            EuiHeaderSectionItemButton
                            EuiPageSideBar
                            EuiDescriptionList
                            EuiDescriptionListTitle
                            EuiDescriptionListDescription
                            EuiTitle
                            EuiInputPopover
                            EuiSearchBar
                            EuiSuggest
                            EuiSuggestItem
                            EuiLoadingContent
                            EuiSelect
                            EuiContextMenu
                            EuiPopover
                            EuiWrappingPopover
                            EuiProvider
                            EuiButton
                            EuiForm
                            EuiFormRow
                            EuiFieldText
                            EuiSwitch
                            EuiFlexGroup
                            EuiFlexGrid
                            EuiFlexItem
                            EuiPageTemplate
                            EuiEmptyPrompt
                            EuiSpacer
                            EuiPage
                            EuiPageHeader
                            EuiPageBody
                            EuiPageContent
                            EuiPageContentBody
                            EuiLoadingSpinner
                            EuiModal
                            EuiModalHeader
                            EuiModalHeaderTitle
                            EuiModalBody
                            EuiModalFooter
                            EuiIcon
                            EuiCard
                            EuiButtonIcon
                            EuiTabs
                            EuiTab
                            EuiConfirmModal
                            EuiFieldPassword
                            EuiFilePicker
                            EuiImage
                            EuiButtonGroup
                            EuiColorPicker
                            EuiListGroup
                            EuiListGroupItem
                            EuiBadge
                            EuiComboBox
                            EuiProgress
                            EuiPanel
                            EuiText
                            EuiSuperSelect]]))

(defn q [selector] (.querySelector js/document selector))
(defn create-cache
  []
  (let [cache (createCache (clj->js {:key "shinsetsu" :container (q "meta[name=\"emotion-styles\"]")}))]
    (set! (. cache -compat) true)
    cache))

(def input-popover (ri/react-factory EuiInputPopover))
(def provider (ri/react-factory EuiProvider))
(def empty-prompt (ri/react-factory EuiEmptyPrompt))
(def spacer (ri/react-factory EuiSpacer))
(def loading-spinner (ri/react-factory EuiLoadingSpinner))
(def icon (ri/react-factory EuiIcon))
(def card (ri/react-factory EuiCard))
(def image (ri/react-factory EuiImage))
(def badge (ri/react-factory EuiBadge))
(def progress (ri/react-factory EuiProgress))
(def panel (ri/react-factory EuiPanel))
(def context-menu (ri/react-factory EuiContextMenu))
(def popover (ri/react-factory EuiPopover))
(def wrapping-popover (ri/react-factory EuiWrappingPopover))
(def text (ri/react-factory EuiText))
(def loading-content (ri/react-factory EuiLoadingContent))
(def title (ri/react-factory EuiTitle))

;; List
(def list-group (ri/react-factory EuiListGroup))
(def list-group-item (ri/react-factory EuiListGroupItem))

;; Form
(def form (ri/react-factory EuiForm))
(def form-row (ri/react-factory EuiFormRow))

;; Form controls
(def field-text (ri/react-factory EuiFieldText))
(def field-search (ri/react-factory EuiFieldSearch))
(def field-password (ri/react-factory EuiFieldPassword))
(def switch (ri/react-factory EuiSwitch))
(def button (ri/react-factory EuiButton))
(def button-group (ri/react-factory EuiButtonGroup))
(def button-icon (ri/react-factory EuiButtonIcon))
(def file-picker (ri/react-factory EuiFilePicker))
(def colour-picker (ri/react-factory EuiColorPicker))
(def combo-box (ri/react-factory EuiComboBox))
(def select (ri/react-factory EuiSelect))
(def suggest (ri/react-factory EuiSuggest))
(def suggest-item (ri/react-factory EuiSuggestItem))
(def search-bar (ri/react-factory EuiSearchBar))
(def super-select (ri/react-factory EuiSuperSelect))
(def search-bar-query-match-all (.-MATCH_ALL (.-Query EuiSearchBar)))
(defn query->EsQuery
  [q]
  (as-> q $
        (.toESQuery (.-Query EuiSearchBar) $)
        (js->clj $ :keywordize-keys true)))

;; Flex
(def flex-group (ri/react-factory EuiFlexGroup))
(def flex-item (ri/react-factory EuiFlexItem))
(def flex-grid (ri/react-factory EuiFlexGrid))

;; Page
(def page (ri/react-factory EuiPage))
(def page-template (ri/react-factory EuiPageTemplate))
(def page-header (ri/react-factory EuiPageHeader))
(def page-body (ri/react-factory EuiPageBody))
(def page-content (ri/react-factory EuiPageContent))
(def page-content-body (ri/react-factory EuiPageContentBody))
(def page-sidebar (ri/react-factory EuiPageSideBar))

;; Modal
(def modal (ri/react-factory EuiModal))
(def modal-header (ri/react-factory EuiModalHeader))
(def modal-header-title (ri/react-factory EuiModalHeaderTitle))
(def modal-body (ri/react-factory EuiModalBody))
(def modal-footer (ri/react-factory EuiModalFooter))
(def confirm-modal (ri/react-factory EuiConfirmModal))

;; Tab
(def tabs (ri/react-factory EuiTabs))
(def tab (ri/react-factory EuiTab))

;; Description list
(def description-list (ri/react-factory EuiDescriptionList))
(def description-list-title (ri/react-factory EuiDescriptionListTitle))
(def description-list-description (ri/react-factory EuiDescriptionListDescription))

;; Header
(def header (ri/react-factory EuiHeader))
(def header-section (ri/react-factory EuiHeaderSection))
(def header-section-item (ri/react-factory EuiHeaderSectionItem))
(def header-section-item-button (ri/react-factory EuiHeaderSectionItemButton))

(def collapsible-nav (ri/react-factory EuiCollapsibleNav))
(def collapsible-nav-group (ri/react-factory EuiCollapsibleNavGroup))
(def horizontal-rule (ri/react-factory EuiHorizontalRule))
