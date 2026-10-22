import {PALETTE} from "../statistic/chartConfig.js";

/**
 * Contains configs for grouping by different time aspects
 */
const TIME_CONFIGS = {
    MONTH: {
        unit: 'month',
        round: 'month',
        displayFormats: {
            month: 'MMM yyyy'
        },
        minUnit: 'month'
    },
    DAY: {
        unit: 'day',
        round: 'day',
        displayFormats: {
            day: 'd. MMM'
        },
        minUnit: 'day'
    }
}

/**
 * Handles the state for a statistics tab and the associated filters
 */
export default class ChartStateHandler {
    #contextElement
    #canvasElement
    #config
    dataUrl
    #filterValueGetters = {}

    constructor(contextElement, dataUrl, config = null) {
        this.#contextElement = contextElement
        this.dataUrl = dataUrl
        this.#config = config
        this.#canvasElement = this.#contextElement.querySelector('.chartCanvas')

        if (!this.#canvasElement) {
            console.error('No canvas element found for container: ' + this.#contextElement)
        }
    }


    /**
     * Fetches the data for the chart from the rest endpoint
     * @returns {Promise<any>}
     */
    async #fetchStatistic() {
        const url = this.#buildUrl()

        const response = await fetch(url)
        if (!response.ok) {
            throw new Error(response.statusText)
        }
        return await response.json();

    }

    /**
     * Destroys any existing chart instance
     */
    destroyChart() {
        const existingChart = Chart.getChart(this.#canvasElement);
        if (existingChart) {
            existingChart.destroy();
        }
    }

    /**
     * Fetches data and renders as chart
     * @returns {Promise<Chart>} the newly rendered ChartJS instance
     */
    async renderChart() {
        const ctx = this.#canvasElement

        if (!ctx) {
            console.error(`could not find canvas element. Will not render chart.`);
            return;
        }

        // destroy any existing instance of a chart
        this.destroyChart(ctx)

        const serverConfig = await this.#fetchStatistic();

        this.#config.data = serverConfig.data;

        for (const dataset of serverConfig.data.datasets) {
            dataset.backgroundColor = PALETTE
        }

        if (serverConfig.timeUnit) {
            this.#setTimeUnit(serverConfig.timeUnit)
        }

        this.#setSumTotal(serverConfig.sum)

        return new Chart(ctx, this.#config);
    }


    /**
     * constructs a url based on the current filters
     * @returns {*} the url including parameters
     */
    #buildUrl() {
        let url = this.dataUrl

        const queryArray = []
        for (const [key, getter] of Object.entries(this.#filterValueGetters)) {
            const value = getter()

            if (value instanceof Array) {
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

    /**
     * Registers a filter for the chart, with an identifier and a function toget the associated value
     * @param identifier identifier for the filter. Is used as parameter key in the data fetching url
     * @param valueGetter function which returns a string or an array of strings. Will be passed as parameters in the url, either as string or as comma seperated string
     */
    registerFilter(identifier, valueGetter) {
        this.#filterValueGetters[identifier] = valueGetter
    }

    /**
     * Sets the time unit for the chart
     * @param timeUnit timeunit recieved from server
     */
    #setTimeUnit(timeUnit) {
        if (!timeUnit) {
            return;
        }
        const config = TIME_CONFIGS[timeUnit]

        if (!config) {
            return;
        }

        this.#config.options.scales.x.time = config
    }

    #setSumTotal(sum) {
        if (sum === null || sum === undefined) {
            return;
        }
        const sumdisplayElement = document.getElementById('stat-summed-total')
        if (!sumdisplayElement) {
            return;
        }

        sumdisplayElement.textContent = sum
    }
}