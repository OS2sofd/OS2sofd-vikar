import "../../vendor/chartjs-adapter-date-fns/chartjs-adapter-date-fns.js"
import ChartStateHandler from "./ChartStateHandler.js";
import configs from "./chartConfig.js"

const FILTER_HANDLERS = {
    fromDate: () => {
    },
    toDate: () => {
    },
    includedOuUuids: () => {
    },
    includedUserIds: () => {
    },
}

document.addEventListener('DOMContentLoaded', () => {
    initChartView()

    initDownloadButton()
})

/**
 * Initializes the chart view
 */
function initChartView() {
    const filterContainer = document.getElementById('filter-container');
    const chartContexts = document.querySelectorAll('.chartView');

    initDatePickers(filterContainer);
    initSelectors(filterContainer);

	const stateHandlers = initCharts(chartContexts)

    initChartButton(filterContainer, stateHandlers);

	initDatePickerHandlers(filterContainer, stateHandlers);
	initSelectorHandlers(filterContainer, stateHandlers);
}

/**
 * Initializes the charts in the chart view
 */
function initCharts(chartContexts) {
    const stateHandlers = []
    for (const context of chartContexts) {
        const configId = context.dataset.configId
        const config = configs[configId]();

        if (!config) {
            console.error(`Could not find chart config with id: ${configId}`);
        }
        const title = context.dataset.title
        if (title) {
            config.options.plugins.title.text = title
        }

        let url = `/rest/statistic/chart/${config.type}`
        const stateHandler = new ChartStateHandler(context, url, config)

        const fieldName = context.dataset.fieldName;
        if (fieldName) {
            stateHandler.registerFilter("fieldName", () => fieldName)
        }

        for (const [identifier, handler] of Object.entries(FILTER_HANDLERS)) {
            stateHandler.registerFilter(identifier, handler)
        }

        stateHandlers.push(stateHandler)
    }
    return stateHandlers
}

/**
 * Initializes the button for generating a chart
 * @param context the container element hosting this button
 * @param stateHandlerArray An array of  ChartStateHandler responsible for rendering charts
 */
function initChartButton(context, stateHandlerArray) {
    const button = context.querySelector('.generateChartButton');
    button.addEventListener('click', () => {
        stateHandlerArray.forEach(stateHandler => stateHandler.renderChart())
    })
}

function initDatePickerHandlers(context, stateHandlerArray) {
	$(".fromDate").on('change', () => {
		stateHandlerArray.forEach(stateHandler => stateHandler.renderChart());
	});

	$(".toDate").on('change', () => {
		stateHandlerArray.forEach(stateHandler => stateHandler.renderChart());
	});
}

function initSelectorHandlers(context, stateHandlerArray) {
	$(".ouSelector").on('change', () => {
		stateHandlerArray.forEach(stateHandler => stateHandler.renderChart());
	});

	$(".userSelector").on('change', () => {
		stateHandlerArray.forEach(stateHandler => stateHandler.renderChart());
	});
}

/**
 * Inits date pickers
 * @param context the container element holding the filters
 */
function initDatePickers(context) {
    const fromDatePicker = context.querySelector('.fromDate')
    initDatePicker(fromDatePicker);
    FILTER_HANDLERS.fromDate = () => fromDatePicker.value

    const toDatePicker = context.querySelector('.toDate')
    initDatePicker(toDatePicker);
    FILTER_HANDLERS.toDate = () => toDatePicker.value
}

/**
 * Inits filtering selectors for this view
 * @param context the container element holding the filters
 */
function initSelectors(context) {
    const ouSelector = context.querySelector('.ouSelector');
    if (ouSelector) {
        initSelect(ouSelector, true);
        FILTER_HANDLERS.includedOuUuids = () => $(ouSelector).select2("val")
    }

    const userSelector = context.querySelector('.userSelector');
    if (userSelector) {
        initSelect(userSelector, true);
        FILTER_HANDLERS.includedUserIds = () => $(userSelector).select2("val")
    }
}

function initDatePicker(element) {
    $(element)?.datepicker({
        keyboardNavigation: false,
        forceParse: false,
        autoclose: true,
        weekStart: 1,
        format: "yyyy-mm-dd",
        orientation: "bottom"
    });
}

function initSelect(element, isMultiple) {
    $(element).select2({multiple: isMultiple})
}

/**
 * Initializes the download statistic button
 */
function initDownloadButton() {
    const BASEURL = `/substituteadmin/download/statistic`
    const buttonID = `download-statistic`
    const buttonElement = document.getElementById(buttonID)
    if (!buttonElement) {
        console.warn(`Could not find download button with ID ${buttonID}. Did not initialize download button.`)
    }
    buttonElement.addEventListener('click', () => {
        const url = buildUrl(BASEURL, FILTER_HANDLERS)

        // creates a temporary anchor element, clicks it and removes it.
        const a = document.createElement('a')
        a.href = url
        document.body.appendChild(a)
        a.click()
        a.remove()
    })
}

/**
 * Builds url with query parameters from the selected filters on the statistic page
 */
function buildUrl(baseUrl, queryHandlerObject) {
    let url = baseUrl
    const queryArray = []
    for (const [key, getter] of Object.entries(queryHandlerObject)) {
        const value = getter()

        if (Array.isArray(value)) {
            if (value.length > 0) {
                queryArray.push(`${key}=${value.join(',')}`);
            }
        } else if (value !== null && value !== undefined && value !== '') {
            queryArray.push(`${key}=${value}`);
        }
    }

    if (queryArray.length > 0) {
        for (let i = 0; i < queryArray.length; i++) {
            if (i === 0) {
                url += '?' + queryArray[i]
            } else {
                url += '&' + queryArray[i]
            }
        }
    }

    return url;
}