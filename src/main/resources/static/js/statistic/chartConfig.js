/**
 * Contains factory functions for the config for different types of charts.
 * Note that some fields may be mutated after the function is called (like title and data)
 */
export default {
    PR_MONTH: () => ({
        type: 'bar',
        data: null,
        options: {
            locale: 'da-DK',
            responsive: true,
            maintainAspectRatio: true,
            plugins: {
                title: {
                    display: true,
                    text: "Vikarer per måned"
                },
                legend: {
                    display: false
                }
            },
            parsing: {
                xAxisKey: 'x',
                yAxisKey: 'y',
                key: "y",
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        unit: 'month',
                        round: 'month',
                        displayFormats: {
                            month: 'MMM yyyy'
                        },
                        minUnit: 'month'
                    },
                    ticks: {
                        autoSkip: false,
                        stepSize: 1,
                    },
                    adapters: {
                        date: {
                            locale: dateFns.locale.da
                        }
                    },
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0,
                        stepSize: 1
                    },
                }
            }
        }
    }),
    DISTRIBUTION: () => ({
        type: 'pie',
        data: null,
        options: {
            locale: 'da-DK',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: {
                    display: true,
                    text: "Fordeling",
                    align: "center",
                    fullSize: false,
                },
                legend: {
                    display: true,
                    position: 'right',
                    align: 'start',
                }
            },
            parsing: {
                xAxisKey: 'x',
                yAxisKey: 'y',
                key: "y",
            },
        }
    })
}

export const PALETTE = [
    "#e6194b", // red
    "#3cb44b", // green
    "#ffe119", // yellow
    "#4363d8", // blue
    "#f58231", // orange
    "#911eb4", // purple
    "#42d4f4", // cyan
    "#f032e6", // magenta
    "#bfef45", // lime
    "#fabed4", // pink
    "#469990", // teal
    "#dcbeff", // lavender
    "#9a6324", // brown
    "#fffac8", // beige
    "#800000", // maroon
    "#aaffc3", // mint
    "#808000", // olive
    "#ffd8b1", // apricot
    "#000075", // navy
    "#a9a9a9"  // grey
];